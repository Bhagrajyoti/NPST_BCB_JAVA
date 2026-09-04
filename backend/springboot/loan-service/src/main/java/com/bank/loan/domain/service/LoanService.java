package com.bank.loan.domain.service;

import com.bank.loan.api.v1.dto.request.ApplyForLoanRequest;
import com.bank.loan.domain.entity.Loan;
import java.util.List;
import java.util.UUID;

public interface LoanService {

    Loan applyForLoan(ApplyForLoanRequest request, String cif, UUID keycloakUserId, String bankCode);

    Loan getLoan(UUID loanId);

    List<Loan> getLoansForCif(String cif);

    /** Runs underwriting: pulls the credit score and auto-approves/rejects, or leaves it for manual review. */
    Loan review(UUID loanId);

    Loan approve(UUID loanId);

    Loan reject(UUID loanId, String reason);

    Loan disburse(UUID loanId);
}
