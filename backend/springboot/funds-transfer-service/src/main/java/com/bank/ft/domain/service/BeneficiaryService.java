package com.bank.ft.domain.service;

import com.bank.ft.api.v1.dto.request.CreateBeneficiaryRequest;
import com.bank.ft.domain.entity.Beneficiary;
import java.util.List;
import java.util.UUID;

public interface BeneficiaryService {

    Beneficiary addBeneficiary(CreateBeneficiaryRequest request, String ownerCif, UUID ownerKeycloakUserId, String bankCode);

    List<Beneficiary> getBeneficiariesForCif(String ownerCif);

    Beneficiary getBeneficiary(UUID beneficiaryId);

    Beneficiary blockBeneficiary(UUID beneficiaryId);
}
