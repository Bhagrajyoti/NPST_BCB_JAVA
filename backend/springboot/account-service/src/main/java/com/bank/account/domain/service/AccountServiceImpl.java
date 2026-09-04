package com.bank.account.domain.service;

import com.bank.account.api.v1.dto.request.CreateAccountRequest;
import com.bank.account.common.exception.ResourceNotFoundException;
import com.bank.account.domain.entity.Account;
import com.bank.account.domain.repository.AccountRepository;
import com.bank.account.domain.statemachine.AccountStateMachine;
import com.bank.account.domain.statemachine.AccountStatus;
import com.bank.account.messaging.outbox.OutboxEvent;
import com.bank.account.messaging.outbox.OutboxEventRepository;
import com.bank.account.messaging.outbox.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountStateMachine accountStateMachine;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Account createAccount(CreateAccountRequest request, UUID keycloakUserId, String bankCode) {
        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .cif(request.cif())
                .keycloakUserId(keycloakUserId)
                .accountType(request.accountType())
                .balanceMinorUnits(BigInteger.ZERO)
                .currency(request.currency())
                .status(AccountStatus.PENDING_ACTIVATION)
                .bankCode(bankCode)
                .build();
        Account saved = accountRepository.save(account);
        enqueueOutboxEvent(saved, "ACCOUNT_OPENED");
        return saved;
    }

    @Override
    public Account getAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
    }

    @Override
    public List<Account> getAccountsForCif(String cif) {
        return accountRepository.findByCif(cif);
    }

    @Override
    @Transactional
    public Account freezeAccount(UUID accountId) {
        return transition(accountId, AccountStatus.FROZEN, "ACCOUNT_FROZEN");
    }

    @Override
    @Transactional
    public Account reactivateAccount(UUID accountId) {
        return transition(accountId, AccountStatus.ACTIVE, "ACCOUNT_REACTIVATED");
    }

    @Override
    @Transactional
    public Account closeAccount(UUID accountId) {
        return transition(accountId, AccountStatus.CLOSED, "ACCOUNT_CLOSED");
    }

    private Account transition(UUID accountId, AccountStatus target, String eventType) {
        Account account = getAccount(accountId);
        accountStateMachine.assertTransitionAllowed(account.getStatus(), target);
        account.setStatus(target);
        Account saved = accountRepository.save(account);
        enqueueOutboxEvent(saved, eventType);
        return saved;
    }

    private void enqueueOutboxEvent(Account account, String eventType) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("ACCOUNT")
                    .aggregateId(account.getId())
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(account))
                    .status(OutboxStatus.PENDING)
                    .createdAt(Instant.now())
                    .build();
            outboxEventRepository.save(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to enqueue outbox event for account " + account.getId(), e);
        }
    }

    private String generateAccountNumber() {
        // Placeholder allocation — a real flow gets the account number back from CbsFeignClient
        // at account-opening time; this keeps the template runnable without a live CBS.
        return "AC" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }
}
