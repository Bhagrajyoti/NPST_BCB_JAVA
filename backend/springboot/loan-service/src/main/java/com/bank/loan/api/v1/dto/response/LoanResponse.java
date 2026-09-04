package com.bank.loan.api.v1.dto.response;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public record LoanResponse(
        UUID id,
        String cif,
        String loanType,
        BigInteger principalMinorUnits,
        Integer interestRateBps,
        Integer tenureMonths,
        BigInteger emiMinorUnits,
        BigInteger outstandingPrincipalMinorUnits,
        String currency,
        String status,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
}
