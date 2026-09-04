package com.bank.ft.api.v1.controller;

import com.bank.ft.api.v1.dto.request.CreateBeneficiaryRequest;
import com.bank.ft.api.v1.dto.response.BeneficiaryResponse;
import com.bank.ft.api.v1.mapper.BeneficiaryMapper;
import com.bank.ft.common.security.IdorGuard;
import com.bank.ft.common.security.keycloak.CurrentUserResolver;
import com.bank.ft.common.security.keycloak.KeycloakRoleConstants;
import com.bank.ft.domain.entity.Beneficiary;
import com.bank.ft.domain.service.BeneficiaryService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final BeneficiaryMapper beneficiaryMapper;
    private final CurrentUserResolver currentUserResolver;
    private final IdorGuard idorGuard;

    @PostMapping
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.CUSTOMER + "')")
    public ResponseEntity<BeneficiaryResponse> addBeneficiary(@Valid @RequestBody CreateBeneficiaryRequest request) {
        Beneficiary beneficiary = beneficiaryService.addBeneficiary(
                request, currentUserResolver.currentCif(), currentUserResolver.currentKeycloakUserId(), TenantContext.get());
        return ResponseEntity.ok(beneficiaryMapper.toResponse(beneficiary));
    }

    @GetMapping
    @PreAuthorize("hasRole('" + KeycloakRoleConstants.CUSTOMER + "')")
    public ResponseEntity<List<BeneficiaryResponse>> getMyBeneficiaries() {
        List<Beneficiary> beneficiaries = beneficiaryService.getBeneficiariesForCif(currentUserResolver.currentCif());
        return ResponseEntity.ok(beneficiaries.stream().map(beneficiaryMapper::toResponse).toList());
    }

    @PutMapping("/{beneficiaryId}/block")
    @PreAuthorize("hasAnyRole('" + KeycloakRoleConstants.CUSTOMER + "', '" + KeycloakRoleConstants.OPS + "')")
    public ResponseEntity<BeneficiaryResponse> blockBeneficiary(@PathVariable UUID beneficiaryId) {
        Beneficiary beneficiary = beneficiaryService.getBeneficiary(beneficiaryId);
        idorGuard.assertOwnedByCurrentUser(beneficiary.getOwnerCif());
        return ResponseEntity.ok(beneficiaryMapper.toResponse(beneficiaryService.blockBeneficiary(beneficiaryId)));
    }
}
