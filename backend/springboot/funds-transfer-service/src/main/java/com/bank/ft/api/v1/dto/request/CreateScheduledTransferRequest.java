package com.bank.ft.api.v1.dto.request;

import com.bank.ft.domain.statemachine.ScheduleFrequency;
import com.bank.ft.domain.statemachine.TransferMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.FutureOrPresent;
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
        @FutureOrPresent
        LocalDate nextExecutionDate,

        @PositiveOrZero
        Integer maxRetries,

        @FutureOrPresent
        LocalDate endDate

        @NotNull
        String bankCode,

        @NotNull
        String transferDescription,

        @NotNull
        String transferReference,

        @NotNull
        String transferType,

        @NotNull
        String transferStatus,

        @NotNull
        String transferReason,

        @NotNull
        String transferReasonCode,
        
        @NotNull
        String transferReasonDescription,

        @NotNull
        String transferReasonCodeDescription,
        
        
        
        
) {
    @AssertTrue(message = "endDate must be null or on/after nextExecutionDate")
    public boolean isEndDateValid() {
        return endDate == null || !endDate.isBefore(nextExecutionDate);
    }
}
