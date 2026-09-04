package com.bank.ft.api.v1.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BeneficiaryResponse(
        UUID id,
        String beneficiaryName,
        String beneficiaryAccountNumber,
        String beneficiaryIfscCode,
        String nickname,
        String transferMode,
        String status,
        Instant coolingPeriodEndsAt
) {
}
