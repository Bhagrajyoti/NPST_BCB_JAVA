package com.bank.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bank.account.api.v1.dto.request.CreateAccountRequest;
import com.bank.account.common.exception.IllegalTransitionException;
import com.bank.account.common.exception.ResourceNotFoundException;
import com.bank.account.domain.entity.Account;
import com.bank.account.domain.repository.AccountRepository;
import com.bank.account.domain.service.AccountServiceImpl;
import com.bank.account.domain.statemachine.AccountStateMachine;
import com.bank.account.domain.statemachine.AccountStatus;
import com.bank.account.messaging.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private AccountStateMachine accountStateMachine;

    @BeforeEach
    void setUp() {
        accountStateMachine = new AccountStateMachine();
        accountService = new AccountServiceImpl(
                accountRepository, outboxEventRepository, accountStateMachine, new ObjectMapper());
    }

    @Test
    void createAccountPersistsPendingActivationAccount() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account account = accountService.createAccount(
                new CreateAccountRequest("1234567890", "SAVINGS", "INR"),
                UUID.randomUUID(),
                "default-bank");

        assertThat(account.getStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        assertThat(account.getBalanceMinorUnits()).isEqualTo(BigInteger.ZERO);
    }

    @Test
    void getAccountThrowsWhenMissing() {
        UUID missingId = UUID.randomUUID();
        when(accountRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(missingId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void closeAccountRejectsIllegalTransitionFromClosed() {
        UUID accountId = UUID.randomUUID();
        Account closedAccount = Account.builder()
                .id(accountId)
                .status(AccountStatus.CLOSED)
                .build();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(closedAccount));

        assertThatThrownBy(() -> accountService.reactivateAccount(accountId))
                .isInstanceOf(IllegalTransitionException.class);
    }
}
