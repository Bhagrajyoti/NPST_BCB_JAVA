package com.bank.account.api.v1.controller;

import com.bank.account.api.v1.dto.request.CreateAccountRequest;
import com.bank.account.api.v1.dto.response.AccountResponse;
import com.bank.account.api.v1.mapper.AccountMapper;
import com.bank.account.common.security.IdorGuard;
import com.bank.account.common.security.keycloak.CurrentUserResolver;
import com.bank.account.common.security.keycloak.KeycloakRoleConstants;
import com.bank.account.domain.entity.Account;
import com.bank.account.domain.service.AccountService;
import com.bank.account.idempotency.Idempotent;
import com.bank.account.tenant.TenantContext;
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
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final CurrentUserResolver currentUserResolver;
    private final IdorGuard idorGuard;

    @PostMapping
    @Idempotent
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.CUSTOMER + "')")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(
                request, currentUserResolver.currentKeycloakUserId(), TenantContext.get());
        return ResponseEntity.ok(accountMapper.toResponse(account));
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('" + KeycloakRoleConstants.CUSTOMER + "', '" + KeycloakRoleConstants.OPS + "')")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID accountId) {
        Account account = accountService.getAccount(accountId);
        idorGuard.assertOwnedByCurrentUser(account.getCif());
        return ResponseEntity.ok(accountMapper.toResponse(account));
    }

    @GetMapping
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.CUSTOMER + "')")
    public ResponseEntity<List<AccountResponse>> getMyAccounts() {
        List<Account> accounts = accountService.getAccountsForCif(currentUserResolver.currentCif());
        return ResponseEntity.ok(accounts.stream().map(accountMapper::toResponse).toList());
    }

    @PutMapping("/{accountId}/freeze")
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.OPS + "')")
    public ResponseEntity<AccountResponse> freezeAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountMapper.toResponse(accountService.freezeAccount(accountId)));
    }

    @PutMapping("/{accountId}/reactivate")
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.OPS + "')")
    public ResponseEntity<AccountResponse> reactivateAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountMapper.toResponse(accountService.reactivateAccount(accountId)));
    }

    @PutMapping("/{accountId}/close")
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.OPS + "')")
    public ResponseEntity<AccountResponse> closeAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountMapper.toResponse(accountService.closeAccount(accountId)));
    }
}
