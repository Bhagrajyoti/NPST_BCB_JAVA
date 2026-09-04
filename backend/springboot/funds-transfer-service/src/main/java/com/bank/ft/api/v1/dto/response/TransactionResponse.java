package com.bank.ft.api.v1.dto.response;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String transactionReference,
        String initiatorCif,
        String destinationAccountNumber,
        String destinationIfscCode,
        BigInteger amountMinorUnits,
        String currency,
        String transferMode,
        String status,
        String failureReason,
        Instant initiatedAt,
        Instant completedAt
) {
}
