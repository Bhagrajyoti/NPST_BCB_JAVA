package com.bank.ft.api.v1.dto.request;

import com.bank.ft.domain.statemachine.ScheduleFrequency;
import com.bank.ft.domain.statemachine.TransferMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.UUID;

public record CreateScheduledTransferRequest(

        @NotNull
        UUID beneficiaryId,

        @NotNull
        @Positive
        BigInteger amountMinorUnits,

        @NotNull
        TransferMode transferMode,

        @NotNull
        ScheduleFrequency frequency,

        @NotNull
        LocalDate nextExecutionDate,

        LocalDate endDate
) {
}
