package com.bank.account.api.v1.dto.response;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        String cif,
        String accountType,
        BigInteger balanceMinorUnits,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
