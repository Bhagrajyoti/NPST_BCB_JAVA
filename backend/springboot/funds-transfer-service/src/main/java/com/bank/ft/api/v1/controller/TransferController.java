package com.bank.ft.api.v1.controller;

import com.bank.ft.api.v1.dto.request.InitiateTransferRequest;
import com.bank.ft.api.v1.dto.response.TransactionResponse;
import com.bank.ft.api.v1.mapper.TransactionMapper;
import com.bank.ft.common.security.IdorGuard;
import com.bank.ft.common.security.keycloak.CurrentUserResolver;
import com.bank.ft.common.security.keycloak.KeycloakRoleConstants;
import com.bank.ft.domain.entity.Transaction;
import com.bank.ft.domain.service.TransferService;
import com.bank.ft.idempotency.Idempotent;
import com.bank.ft.tenant.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final TransactionMapper transactionMapper;
    private final CurrentUserResolver currentUserResolver;
    private final IdorGuard idorGuard;

    @PostMapping
    @Idempotent
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.CUSTOMER + "')")
    public ResponseEntity<TransactionResponse> initiateTransfer(
            @Valid @RequestBody InitiateTransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        Transaction transaction = transferService.initiateTransfer(
                request,
                currentUserResolver.currentCif(),
                currentUserResolver.currentKeycloakUserId(),
                TenantContext.get(),
                idempotencyKey);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('" + KeycloakRoleConstants.CUSTOMER + "', '" + KeycloakRoleConstants.OPS + "')")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable UUID transactionId) {
        Transaction transaction = transferService.getTransaction(transactionId);
        idorGuard.assertOwnedByCurrentUser(transaction.getInitiatorCif());
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @GetMapping
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.CUSTOMER + "')")
    public ResponseEntity<List<TransactionResponse>> getMyTransfers() {
        List<Transaction> transactions = transferService.getTransactionsForCif(currentUserResolver.currentCif());
        return ResponseEntity.ok(transactions.stream().map(transactionMapper::toResponse).toList());
    }
}
