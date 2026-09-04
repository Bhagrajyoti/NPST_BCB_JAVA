package com.bank.ft.domain.service;

import com.bank.ft.api.v1.dto.request.InitiateTransferRequest;
import com.bank.ft.client.feign.CbsFeignClient;
import com.bank.ft.client.feign.SwitchFeignClient;
import com.bank.ft.common.exception.ResourceNotFoundException;
import com.bank.ft.domain.entity.Transaction;
import com.bank.ft.domain.repository.TransactionRepository;
import com.bank.ft.domain.statemachine.TransactionStateMachine;
import com.bank.ft.domain.statemachine.TransactionStatus;
import com.bank.ft.domain.statemachine.TransferMode;
import com.bank.ft.messaging.outbox.OutboxEvent;
import com.bank.ft.messaging.outbox.OutboxEventRepository;
import com.bank.ft.messaging.outbox.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates a transfer: debit via CBS, submit via the NPCI switch,
 * advance the state machine, and enqueue the completion notification
 * through the outbox — never a synchronous notification call inside this
 * transaction's critical path.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferServiceImpl implements TransferService {

    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TransactionStateMachine transactionStateMachine;
    private final CbsFeignClient cbsFeignClient;
    private final SwitchFeignClient switchFeignClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Transaction initiateTransfer(InitiateTransferRequest request, String initiatorCif,
                                         UUID initiatorKeycloakUserId, String bankCode, String idempotencyKey) {
        Transaction transaction = Transaction.builder()
                .transactionReference(generateTransactionReference())
                .idempotencyKey(idempotencyKey)
                .initiatorCif(initiatorCif)
                .initiatorKeycloakUserId(initiatorKeycloakUserId)
                .beneficiaryId(request.beneficiaryId())
                .destinationAccountNumber(request.destinationAccountNumber())
                .destinationIfscCode(request.destinationIfscCode())
                .amountMinorUnits(request.amountMinorUnits())
                .currency(request.currency())
                .transferMode(TransferMode.valueOf(request.transferMode()))
                .status(TransactionStatus.INITIATED)
                .remarks(request.remarks())
                .bankCode(bankCode)
                .initiatedAt(Instant.now())
                .build();
        transaction = transactionRepository.save(transaction);

        advance(transaction, TransactionStatus.PENDING);
        processTransfer(transaction);
        return transaction;
    }

    private void processTransfer(Transaction transaction) {
        advance(transaction, TransactionStatus.PROCESSING);

        CbsFeignClient.DebitResult debitResult;
        try {
            debitResult = cbsFeignClient.debit(transaction.getInitiatorCif(), transaction.getAmountMinorUnits());
        } catch (Exception e) {
            failTransfer(transaction, "CBS_DEBIT_FAILED: " + e.getMessage());
            return;
        }
        if (!debitResult.success()) {
            failTransfer(transaction, debitResult.failureReason());
            return;
        }
        transaction.setCbsReferenceNumber(debitResult.cbsReferenceNumber());

        SwitchFeignClient.SwitchTransferResult switchResult = switchFeignClient.submitTransfer(
                new SwitchFeignClient.SwitchTransferRequest(
                        transaction.getTransactionReference(),
                        transaction.getInitiatorCif(),
                        transaction.getDestinationAccountNumber(),
                        transaction.getDestinationIfscCode(),
                        transaction.getAmountMinorUnits(),
                        transaction.getTransferMode().name()));

        if (!switchResult.accepted()) {
            failTransfer(transaction, switchResult.failureReason());
            return;
        }

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setCompletedAt(Instant.now());
        transactionRepository.save(transaction);
        enqueueOutboxEvent(transaction, "TRANSACTION_COMPLETED");
    }

    private void failTransfer(Transaction transaction, String reason) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(reason);
        transaction.setCompletedAt(Instant.now());
        transactionRepository.save(transaction);
        enqueueOutboxEvent(transaction, "TRANSACTION_FAILED");
    }

    private void advance(Transaction transaction, TransactionStatus target) {
        transactionStateMachine.assertTransitionAllowed(transaction.getStatus(), target);
        transaction.setStatus(target);
        transactionRepository.save(transaction);
    }

    @Override
    public Transaction getTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
    }

    @Override
    public List<Transaction> getTransactionsForCif(String initiatorCif) {
        return transactionRepository.findByInitiatorCifOrderByInitiatedAtDesc(initiatorCif);
    }

    private void enqueueOutboxEvent(Transaction transaction, String eventType) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("TRANSACTION")
                    .aggregateId(transaction.getId())
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(transaction))
                    .status(OutboxStatus.PENDING)
                    .createdAt(Instant.now())
                    .build();
            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to enqueue outbox event for transaction {}", transaction.getId(), e);
        }
    }

    private String generateTransactionReference() {
        return "TXN" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }
}
