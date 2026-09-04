package com.bank.loan.domain.service;

import com.bank.loan.api.v1.dto.request.ApplyForLoanRequest;
import com.bank.loan.client.feign.CbsFeignClient;
import com.bank.loan.client.feign.CreditBureauFeignClient;
import com.bank.loan.common.exception.CbsUnavailableException;
import com.bank.loan.common.exception.ResourceNotFoundException;
import com.bank.loan.domain.entity.Loan;
import com.bank.loan.domain.repository.LoanRepository;
import com.bank.loan.domain.statemachine.LoanStateMachine;
import com.bank.loan.domain.statemachine.LoanStatus;
import com.bank.loan.messaging.outbox.OutboxEvent;
import com.bank.loan.messaging.outbox.OutboxEventRepository;
import com.bank.loan.messaging.outbox.OutboxStatus;
import com.bank.loan.tenant.BankConfigProperties;
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
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final LoanStateMachine loanStateMachine;
    private final BankConfigProperties bankConfigProperties;
    private final CbsFeignClient cbsFeignClient;
    private final CreditBureauFeignClient creditBureauFeignClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Loan applyForLoan(ApplyForLoanRequest request, String cif, UUID keycloakUserId, String bankCode) {
        Loan loan = Loan.builder()
                .cif(cif)
                .keycloakUserId(keycloakUserId)
                .loanType(request.loanType())
                .disbursementAccountNumber(request.disbursementAccountNumber())
                .principalMinorUnits(request.principalMinorUnits())
                .interestRateBps(bankConfigProperties.forCode(bankCode).getDefaultInterestRateBps())
                .tenureMonths(request.tenureMonths())
                .currency(request.currency())
                .status(LoanStatus.APPLIED)
                .bankCode(bankCode)
                .build();
        Loan saved = loanRepository.save(loan);
        enqueueOutboxEvent(saved, "LOAN_APPLIED");
        return saved;
    }

    @Override
    public Loan getLoan(UUID loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
    }

    @Override
    public List<Loan> getLoansForCif(String cif) {
        return loanRepository.findByCif(cif);
    }

    @Override
    @Transactional
    public Loan review(UUID loanId) {
        Loan loan = getLoan(loanId);
        loanStateMachine.assertTransitionAllowed(loan.getStatus(), LoanStatus.UNDER_REVIEW);
        loan.setStatus(LoanStatus.UNDER_REVIEW);
        loanRepository.save(loan);

        CreditBureauFeignClient.CreditScoreResult creditScore = creditBureauFeignClient.fetchCreditScore(loan.getCif());
        int minScore = bankConfigProperties.forCode(loan.getBankCode()).getMinCreditScoreForAutoApproval();
        if (creditScore.available() && creditScore.score() >= minScore) {
            return approve(loanId);
        }
        // Bureau unavailable, or below the auto-approval threshold: stays UNDER_REVIEW for a human Checker.
        return loan;
    }

    @Override
    @Transactional
    public Loan approve(UUID loanId) {
        Loan loan = getLoan(loanId);
        loanStateMachine.assertTransitionAllowed(loan.getStatus(), LoanStatus.APPROVED);
        loan.setStatus(LoanStatus.APPROVED);
        loan.setEmiMinorUnits(computeEmi(loan.getPrincipalMinorUnits(), loan.getInterestRateBps(), loan.getTenureMonths()));
        Loan saved = loanRepository.save(loan);
        enqueueOutboxEvent(saved, "LOAN_APPROVED");
        return saved;
    }

    @Override
    @Transactional
    public Loan reject(UUID loanId, String reason) {
        Loan loan = getLoan(loanId);
        loanStateMachine.assertTransitionAllowed(loan.getStatus(), LoanStatus.REJECTED);
        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectionReason(reason);
        Loan saved = loanRepository.save(loan);
        enqueueOutboxEvent(saved, "LOAN_REJECTED");
        return saved;
    }

    @Override
    @Transactional
    public Loan disburse(UUID loanId) {
        Loan loan = getLoan(loanId);
        loanStateMachine.assertTransitionAllowed(loan.getStatus(), LoanStatus.DISBURSED);

        CbsFeignClient.CbsResult result = cbsFeignClient.disburse(loan.getDisbursementAccountNumber(), loan.getPrincipalMinorUnits());
        if (!result.success()) {
            throw new CbsUnavailableException("Unable to disburse loan " + loanId + ": " + result.failureReason());
        }

        loan.setStatus(LoanStatus.DISBURSED);
        loan.setOutstandingPrincipalMinorUnits(loan.getPrincipalMinorUnits());
        Loan saved = loanRepository.save(loan);
        enqueueOutboxEvent(saved, "LOAN_DISBURSED");

        loanStateMachine.assertTransitionAllowed(saved.getStatus(), LoanStatus.ACTIVE);
        saved.setStatus(LoanStatus.ACTIVE);
        return loanRepository.save(saved);
    }

    private BigInteger computeEmi(BigInteger principal, int interestRateBps, int tenureMonths) {
        // Simplified flat-rate EMI for template purposes: (principal + simple interest) / tenureMonths.
        BigInteger totalInterest = principal
                .multiply(BigInteger.valueOf(interestRateBps))
                .multiply(BigInteger.valueOf(tenureMonths))
                .divide(BigInteger.valueOf(10_000L * 12));
        return principal.add(totalInterest).divide(BigInteger.valueOf(tenureMonths));
    }

    private void enqueueOutboxEvent(Loan loan, String eventType) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("LOAN")
                    .aggregateId(loan.getId())
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(loan))
                    .status(OutboxStatus.PENDING)
                    .createdAt(Instant.now())
                    .build();
            outboxEventRepository.save(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to enqueue outbox event for loan " + loan.getId(), e);
        }
    }
}
