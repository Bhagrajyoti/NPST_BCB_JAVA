package com.bank.loan.api.v1.controller;

import com.bank.loan.api.v1.dto.request.ApplyForLoanRequest;
import com.bank.loan.api.v1.dto.response.LoanResponse;
import com.bank.loan.api.v1.mapper.LoanMapper;
import com.bank.loan.common.security.IdorGuard;
import com.bank.loan.common.security.keycloak.CurrentUserResolver;
import com.bank.loan.common.security.keycloak.KeycloakRoleConstants;
import com.bank.loan.domain.entity.Loan;
import com.bank.loan.domain.service.LoanService;
import com.bank.loan.idempotency.Idempotent;
import com.bank.loan.tenant.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final LoanMapper loanMapper;
    private final CurrentUserResolver currentUserResolver;
    private final IdorGuard idorGuard;

    @PostMapping
    @Idempotent
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.CUSTOMER + "')")
    public ResponseEntity<LoanResponse> applyForLoan(@Valid @RequestBody ApplyForLoanRequest request) {
        Loan loan = loanService.applyForLoan(
                request, currentUserResolver.currentCif(), currentUserResolver.currentKeycloakUserId(), TenantContext.get());
        return ResponseEntity.ok(loanMapper.toResponse(loan));
    }

    @GetMapping("/{loanId}")
    @PreAuthorize("hasAnyRole('" + KeycloakRoleConstants.CUSTOMER + "', '" + KeycloakRoleConstants.OPS + "')")
    public ResponseEntity<LoanResponse> getLoan(@PathVariable UUID loanId) {
        Loan loan = loanService.getLoan(loanId);
        idorGuard.assertOwnedByCurrentUser(loan.getCif());
        return ResponseEntity.ok(loanMapper.toResponse(loan));
    }

    @GetMapping
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.CUSTOMER + "')")
    public ResponseEntity<List<LoanResponse>> getMyLoans() {
        List<Loan> loans = loanService.getLoansForCif(currentUserResolver.currentCif());
        return ResponseEntity.ok(loans.stream().map(loanMapper::toResponse).toList());
    }

    @PutMapping("/{loanId}/review")
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.OPS + "')")
    public ResponseEntity<LoanResponse> review(@PathVariable UUID loanId) {
        return ResponseEntity.ok(loanMapper.toResponse(loanService.review(loanId)));
    }

    @PutMapping("/{loanId}/approve")
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.CHECKER + "')")
    public ResponseEntity<LoanResponse> approve(@PathVariable UUID loanId) {
        return ResponseEntity.ok(loanMapper.toResponse(loanService.approve(loanId)));
    }

    @PutMapping("/{loanId}/reject")
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.CHECKER + "')")
    public ResponseEntity<LoanResponse> reject(@PathVariable UUID loanId, @RequestParam String reason) {
        return ResponseEntity.ok(loanMapper.toResponse(loanService.reject(loanId, reason)));
    }

    @PutMapping("/{loanId}/disburse")
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.OPS + "')")
    public ResponseEntity<LoanResponse> disburse(@PathVariable UUID loanId) {
        return ResponseEntity.ok(loanMapper.toResponse(loanService.disburse(loanId)));
    }
}
