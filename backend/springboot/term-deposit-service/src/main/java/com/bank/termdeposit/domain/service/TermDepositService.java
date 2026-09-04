package com.bank.termdeposit.domain.service;

import com.bank.termdeposit.api.v1.dto.request.OpenTermDepositRequest;
import com.bank.termdeposit.domain.entity.TermDeposit;
import java.util.List;
import java.util.UUID;

public interface TermDepositService {

    TermDeposit openDeposit(OpenTermDepositRequest request, String cif, UUID keycloakUserId, String bankCode);

    TermDeposit getDeposit(UUID depositId);

    List<TermDeposit> getDepositsForCif(String cif);

    TermDeposit closePrematurely(UUID depositId);

    /** Called by the maturity scheduler — never invoked directly from a customer-facing endpoint. */
    TermDeposit processMaturity(UUID depositId);
}
