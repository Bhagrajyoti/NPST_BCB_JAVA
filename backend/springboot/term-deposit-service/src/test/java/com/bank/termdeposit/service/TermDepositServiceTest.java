package com.bank.termdeposit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bank.termdeposit.api.v1.dto.request.OpenTermDepositRequest;
import com.bank.termdeposit.client.feign.CbsFeignClient;
import com.bank.termdeposit.common.exception.CbsUnavailableException;
import com.bank.termdeposit.domain.entity.TermDeposit;
import com.bank.termdeposit.domain.repository.TermDepositRepository;
import com.bank.termdeposit.domain.service.TermDepositServiceImpl;
import com.bank.termdeposit.domain.statemachine.TermDepositStateMachine;
import com.bank.termdeposit.domain.statemachine.TermDepositStatus;
import com.bank.termdeposit.messaging.outbox.OutboxEventRepository;
import com.bank.termdeposit.tenant.BankConfigProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TermDepositServiceTest {

    @Mock
    private TermDepositRepository termDepositRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private CbsFeignClient cbsFeignClient;

    private BankConfigProperties bankConfigProperties;
    private TermDepositServiceImpl termDepositService;

    @BeforeEach
    void setUp() {
        bankConfigProperties = new BankConfigProperties();
        BankConfigProperties.Tenant tenant = new BankConfigProperties.Tenant();
        tenant.setDefaultInterestRateBps(650);
        bankConfigProperties.setTenants(java.util.Map.of("default-bank", tenant));

        termDepositService = new TermDepositServiceImpl(
                termDepositRepository,
                outboxEventRepository,
                new TermDepositStateMachine(),
                bankConfigProperties,
                cbsFeignClient,
                new ObjectMapper());
        when(termDepositRepository.save(any(TermDeposit.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void openDepositComputesMaturityAmountWithSimpleInterest() {
        when(cbsFeignClient.debit(anyString(), any(BigInteger.class)))
                .thenReturn(new CbsFeignClient.CbsResult(true, "CBS-REF-1", null));

        TermDeposit deposit = termDepositService.openDeposit(
                new OpenTermDepositRequest("1234567890123", BigInteger.valueOf(1_000_000), 12, "INR", false),
                "9999999999", UUID.randomUUID(), "default-bank");

        assertThat(deposit.getStatus()).isEqualTo(TermDepositStatus.ACTIVE);
        // principal 1,000,000 * 6.50% * (12/12) = 65,000 interest -> maturity 1,065,000
        assertThat(deposit.getMaturityAmountMinorUnits()).isEqualTo(BigInteger.valueOf(1_065_000));
    }

    @Test
    void openDepositThrowsWhenCbsDebitFails() {
        when(cbsFeignClient.debit(anyString(), any(BigInteger.class)))
                .thenReturn(new CbsFeignClient.CbsResult(false, null, "INSUFFICIENT_FUNDS"));

        assertThatThrownBy(() -> termDepositService.openDeposit(
                new OpenTermDepositRequest("1234567890123", BigInteger.valueOf(1_000_000), 12, "INR", false),
                "9999999999", UUID.randomUUID(), "default-bank"))
                .isInstanceOf(CbsUnavailableException.class);
    }

    @Test
    void closePrematurelyRejectsAlreadyClosedDeposit() {
        UUID depositId = UUID.randomUUID();
        TermDeposit closed = TermDeposit.builder().id(depositId).status(TermDepositStatus.CLOSED).build();
        when(termDepositRepository.findById(depositId)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> termDepositService.closePrematurely(depositId))
                .isInstanceOf(com.bank.termdeposit.common.exception.IllegalTransitionException.class);
    }
}
