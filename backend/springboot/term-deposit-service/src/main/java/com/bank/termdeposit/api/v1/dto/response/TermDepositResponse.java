package com.bank.termdeposit.api.v1.dto.response;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TermDepositResponse(
        UUID id,
        String cif,
        String sourceAccountNumber,
        BigInteger principalMinorUnits,
        Integer interestRateBps,
        Integer tenureMonths,
        LocalDate maturityDate,
        BigInteger maturityAmountMinorUnits,
        boolean autoRenew,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
