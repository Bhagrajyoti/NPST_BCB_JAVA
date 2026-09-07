package com.bank.ft.api.v1.dto.response;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ScheduledTransferResponse(
        UUID id,
        String cif,
        UUID beneficiaryId,
        BigInteger amountMinorUnits,
        String transferMode,
        String frequency,
        LocalDate nextExecutionDate,
        LocalDate endDate,
        String status,
        String lastExecutionStatus,
        Instant lastExecutedAt,
        int retryCount,
        int maxRetries,
        Instant createdAt,
        Instant updatedAt
) {
}
