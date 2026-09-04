package com.bank.termdeposit.api.v1.controller;

import com.bank.termdeposit.api.v1.dto.request.OpenTermDepositRequest;
import com.bank.termdeposit.api.v1.dto.response.TermDepositResponse;
import com.bank.termdeposit.api.v1.mapper.TermDepositMapper;
import com.bank.termdeposit.common.security.IdorGuard;
import com.bank.termdeposit.common.security.keycloak.CurrentUserResolver;
import com.bank.termdeposit.common.security.keycloak.KeycloakRoleConstants;
import com.bank.termdeposit.domain.entity.TermDeposit;
import com.bank.termdeposit.domain.service.TermDepositService;
import com.bank.termdeposit.idempotency.Idempotent;
import com.bank.termdeposit.tenant.TenantContext;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/term-deposits")
@RequiredArgsConstructor
public class TermDepositController {

    private final TermDepositService termDepositService;
    private final TermDepositMapper termDepositMapper;
    private final CurrentUserResolver currentUserResolver;
    private final IdorGuard idorGuard;

    @PostMapping
    @Idempotent
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.CUSTOMER + "')")
    public ResponseEntity<TermDepositResponse> openDeposit(@Valid @RequestBody OpenTermDepositRequest request) {
        TermDeposit deposit = termDepositService.openDeposit(
                request, currentUserResolver.currentCif(), currentUserResolver.currentKeycloakUserId(), TenantContext.get());
        return ResponseEntity.ok(termDepositMapper.toResponse(deposit));
    }

    @GetMapping("/{depositId}")
    @PreAuthorize("hasAnyRole('" + KeycloakRoleConstants.CUSTOMER + "', '" + KeycloakRoleConstants.OPS + "')")
    public ResponseEntity<TermDepositResponse> getDeposit(@PathVariable UUID depositId) {
        TermDeposit deposit = termDepositService.getDeposit(depositId);
        idorGuard.assertOwnedByCurrentUser(deposit.getCif());
        return ResponseEntity.ok(termDepositMapper.toResponse(deposit));
    }

    @GetMapping
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.CUSTOMER + "')")
    public ResponseEntity<List<TermDepositResponse>> getMyDeposits() {
        List<TermDeposit> deposits = termDepositService.getDepositsForCif(currentUserResolver.currentCif());
        return ResponseEntity.ok(deposits.stream().map(termDepositMapper::toResponse).toList());
    }

    @PutMapping("/{depositId}/close-prematurely")
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.CUSTOMER + "')")
    public ResponseEntity<TermDepositResponse> closePrematurely(@PathVariable UUID depositId) {
        TermDeposit deposit = termDepositService.getDeposit(depositId);
        idorGuard.assertOwnedByCurrentUser(deposit.getCif());
        return ResponseEntity.ok(termDepositMapper.toResponse(termDepositService.closePrematurely(depositId)));
    }
}
