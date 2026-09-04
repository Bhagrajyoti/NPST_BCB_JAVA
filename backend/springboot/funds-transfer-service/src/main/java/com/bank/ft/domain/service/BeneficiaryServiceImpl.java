package com.bank.ft.domain.service;

import com.bank.ft.api.v1.dto.request.CreateBeneficiaryRequest;
import com.bank.ft.common.exception.IllegalTransitionException;
import com.bank.ft.common.exception.ResourceNotFoundException;
import com.bank.ft.domain.entity.Beneficiary;
import com.bank.ft.domain.repository.BeneficiaryRepository;
import com.bank.ft.domain.statemachine.BeneficiaryStatus;
import com.bank.ft.domain.statemachine.TransferMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BeneficiaryServiceImpl implements BeneficiaryService {

    // Confirmed as a fixed default here; if per-bank cooling-period duration is required,
    // move this to BankConfigProperties.Tenant — see the open item in the schema doc.
    private static final long COOLING_PERIOD_HOURS = 24;

    private final BeneficiaryRepository beneficiaryRepository;

    @Override
    @Transactional
    public Beneficiary addBeneficiary(
            CreateBeneficiaryRequest request, String ownerCif, UUID ownerKeycloakUserId, String bankCode) {
        Beneficiary beneficiary = Beneficiary.builder()
                .ownerCif(ownerCif)
                .ownerKeycloakUserId(ownerKeycloakUserId)
                .beneficiaryName(request.beneficiaryName())
                .beneficiaryAccountNumber(request.beneficiaryAccountNumber())
                .beneficiaryIfscCode(request.beneficiaryIfscCode())
                .nickname(request.nickname())
                .transferMode(TransferMode.valueOf(request.transferMode()))
                .status(BeneficiaryStatus.PENDING_COOLING_PERIOD)
                .coolingPeriodEndsAt(Instant.now().plusSeconds(COOLING_PERIOD_HOURS * 3600))
                .bankCode(bankCode)
                .build();
        return beneficiaryRepository.save(beneficiary);
    }

    @Override
    public List<Beneficiary> getBeneficiariesForCif(String ownerCif) {
        return beneficiaryRepository.findByOwnerCif(ownerCif);
    }

    @Override
    public Beneficiary getBeneficiary(UUID beneficiaryId) {
        return beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found: " + beneficiaryId));
    }

    @Override
    @Transactional
    public Beneficiary blockBeneficiary(UUID beneficiaryId) {
        Beneficiary beneficiary = getBeneficiary(beneficiaryId);
        if (beneficiary.getStatus() == BeneficiaryStatus.DELETED) {
            throw new IllegalTransitionException("Cannot block a deleted beneficiary: " + beneficiaryId);
        }
        beneficiary.setStatus(BeneficiaryStatus.BLOCKED);
        return beneficiaryRepository.save(beneficiary);
    }
}
