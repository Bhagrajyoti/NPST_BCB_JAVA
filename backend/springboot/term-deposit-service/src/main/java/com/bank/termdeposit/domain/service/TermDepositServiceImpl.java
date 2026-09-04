package com.bank.termdeposit.domain.service;

import com.bank.termdeposit.api.v1.dto.request.OpenTermDepositRequest;
import com.bank.termdeposit.client.feign.CbsFeignClient;
import com.bank.termdeposit.common.exception.CbsUnavailableException;
import com.bank.termdeposit.common.exception.ResourceNotFoundException;
import com.bank.termdeposit.domain.entity.TermDeposit;
import com.bank.termdeposit.domain.repository.TermDepositRepository;
import com.bank.termdeposit.domain.statemachine.TermDepositStateMachine;
import com.bank.termdeposit.domain.statemachine.TermDepositStatus;
import com.bank.termdeposit.messaging.outbox.OutboxEvent;
import com.bank.termdeposit.messaging.outbox.OutboxEventRepository;
import com.bank.termdeposit.messaging.outbox.OutboxStatus;
import com.bank.termdeposit.tenant.BankConfigProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TermDepositServiceImpl implements TermDepositService {

    private final TermDepositRepository termDepositRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TermDepositStateMachine termDepositStateMachine;
    private final BankConfigProperties bankConfigProperties;
    private final CbsFeignClient cbsFeignClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TermDeposit openDeposit(OpenTermDepositRequest request, String cif, UUID keycloakUserId, String bankCode) {
        int interestRateBps = bankConfigProperties.forCode(bankCode).getDefaultInterestRateBps();
        LocalDate maturityDate = LocalDate.now().plusMonths(request.tenureMonths());
        BigInteger maturityAmount = computeMaturityAmount(
                request.principalMinorUnits(), interestRateBps, request.tenureMonths());

        CbsFeignClient.CbsResult debitResult = cbsFeignClient.debit(
                request.sourceAccountNumber(), request.principalMinorUnits());
        if (!debitResult.success()) {
            throw new CbsUnavailableException("Unable to fund term deposit: " + debitResult.failureReason());
        }

        TermDeposit deposit = TermDeposit.builder()
                .cif(cif)
                .keycloakUserId(keycloakUserId)
                .sourceAccountNumber(request.sourceAccountNumber())
                .principalMinorUnits(request.principalMinorUnits())
                .interestRateBps(interestRateBps)
                .tenureMonths(request.tenureMonths())
                .maturityDate(maturityDate)
                .maturityAmountMinorUnits(maturityAmount)
                .autoRenew(request.autoRenew())
                .currency(request.currency())
                .status(TermDepositStatus.ACTIVE)
                .bankCode(bankCode)
                .build();
        TermDeposit saved = termDepositRepository.save(deposit);
        enqueueOutboxEvent(saved, "TERM_DEPOSIT_OPENED");
        return saved;
    }

    @Override
    public TermDeposit getDeposit(UUID depositId) {
        return termDepositRepository.findById(depositId)
                .orElseThrow(() -> new ResourceNotFoundException("Term deposit not found: " + depositId));
    }

    @Override
    public List<TermDeposit> getDepositsForCif(String cif) {
        return termDepositRepository.findByCif(cif);
    }

    @Override
    @Transactional
    public TermDeposit closePrematurely(UUID depositId) {
        TermDeposit deposit = getDeposit(depositId);
        termDepositStateMachine.assertTransitionAllowed(deposit.getStatus(), TermDepositStatus.PREMATURELY_CLOSED);
        payout(deposit, deposit.getPrincipalMinorUnits()); // simplified: real payout logic applies a penalty rate
        deposit.setStatus(TermDepositStatus.PREMATURELY_CLOSED);
        TermDeposit saved = termDepositRepository.save(deposit);
        enqueueOutboxEvent(saved, "TERM_DEPOSIT_CLOSED_PREMATURELY");
        return saved;
    }

    @Override
    @Transactional
    public TermDeposit processMaturity(UUID depositId) {
        TermDeposit deposit = getDeposit(depositId);
        termDepositStateMachine.assertTransitionAllowed(deposit.getStatus(), TermDepositStatus.MATURED);
        payout(deposit, deposit.getMaturityAmountMinorUnits());
        deposit.setStatus(TermDepositStatus.MATURED);
        TermDeposit saved = termDepositRepository.save(deposit);
        enqueueOutboxEvent(saved, "TERM_DEPOSIT_MATURED");
        return saved;
    }

    private void payout(TermDeposit deposit, BigInteger amountMinorUnits) {
        CbsFeignClient.CbsResult creditResult = cbsFeignClient.credit(deposit.getSourceAccountNumber(), amountMinorUnits);
        if (!creditResult.success()) {
            throw new CbsUnavailableException("Unable to pay out term deposit " + deposit.getId() + ": " + creditResult.failureReason());
        }
    }

    private BigInteger computeMaturityAmount(BigInteger principal, int interestRateBps, int tenureMonths) {
        // Simple interest for template purposes: principal * rate * (tenureMonths / 12)
        BigInteger interest = principal
                .multiply(BigInteger.valueOf(interestRateBps))
                .multiply(BigInteger.valueOf(tenureMonths))
                .divide(BigInteger.valueOf(10_000L * 12));
        return principal.add(interest);
    }

    private void enqueueOutboxEvent(TermDeposit deposit, String eventType) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("TERM_DEPOSIT")
                    .aggregateId(deposit.getId())
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(deposit))
                    .status(OutboxStatus.PENDING)
                    .createdAt(Instant.now())
                    .build();
            outboxEventRepository.save(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to enqueue outbox event for term deposit " + deposit.getId(), e);
        }
    }
}
