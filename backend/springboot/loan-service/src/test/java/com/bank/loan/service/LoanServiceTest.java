package com.bank.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bank.loan.api.v1.dto.request.ApplyForLoanRequest;
import com.bank.loan.client.feign.CbsFeignClient;
import com.bank.loan.client.feign.CreditBureauFeignClient;
import com.bank.loan.domain.entity.Loan;
import com.bank.loan.domain.repository.LoanRepository;
import com.bank.loan.domain.service.LoanServiceImpl;
import com.bank.loan.domain.statemachine.LoanStateMachine;
import com.bank.loan.domain.statemachine.LoanStatus;
import com.bank.loan.messaging.outbox.OutboxEventRepository;
import com.bank.loan.tenant.BankConfigProperties;
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
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private CbsFeignClient cbsFeignClient;

    @Mock
    private CreditBureauFeignClient creditBureauFeignClient;

    private BankConfigProperties bankConfigProperties;
    private LoanServiceImpl loanService;

    @BeforeEach
    void setUp() {
        bankConfigProperties = new BankConfigProperties();
        BankConfigProperties.Tenant tenant = new BankConfigProperties.Tenant();
        tenant.setDefaultInterestRateBps(1200);
        tenant.setMinCreditScoreForAutoApproval(700);
        bankConfigProperties.setTenants(java.util.Map.of("default-bank", tenant));

        loanService = new LoanServiceImpl(
                loanRepository,
                outboxEventRepository,
                new LoanStateMachine(),
                bankConfigProperties,
                cbsFeignClient,
                creditBureauFeignClient,
                new ObjectMapper());
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void reviewAutoApprovesWhenCreditScoreMeetsThreshold() {
        UUID loanId = UUID.randomUUID();
        Loan applied = Loan.builder()
                .id(loanId)
                .cif("9999999999")
                .status(LoanStatus.APPLIED)
                .principalMinorUnits(BigInteger.valueOf(500_000))
                .interestRateBps(1200)
                .tenureMonths(24)
                .bankCode("default-bank")
                .build();
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(applied));
        when(creditBureauFeignClient.fetchCreditScore("9999999999"))
                .thenReturn(new CreditBureauFeignClient.CreditScoreResult(true, 750));

        Loan result = loanService.review(loanId);

        assertThat(result.getStatus()).isEqualTo(LoanStatus.APPROVED);
        assertThat(result.getEmiMinorUnits()).isNotNull();
    }

    @Test
    void reviewStaysUnderReviewWhenBureauUnavailable() {
        UUID loanId = UUID.randomUUID();
        Loan applied = Loan.builder()
                .id(loanId)
                .cif("9999999999")
                .status(LoanStatus.APPLIED)
                .principalMinorUnits(BigInteger.valueOf(500_000))
                .bankCode("default-bank")
                .build();
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(applied));
        when(creditBureauFeignClient.fetchCreditScore("9999999999"))
                .thenReturn(new CreditBureauFeignClient.CreditScoreResult(false, null));

        Loan result = loanService.review(loanId);

        assertThat(result.getStatus()).isEqualTo(LoanStatus.UNDER_REVIEW);
    }

    @Test
    void applyForLoanCreatesApplicationInAppliedStatus() {
        Loan loan = loanService.applyForLoan(
                new ApplyForLoanRequest("PERSONAL", "1234567890123", BigInteger.valueOf(500_000), 24, "INR"),
                "9999999999", UUID.randomUUID(), "default-bank");

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.APPLIED);
        assertThat(loan.getInterestRateBps()).isEqualTo(1200);
    }
}
