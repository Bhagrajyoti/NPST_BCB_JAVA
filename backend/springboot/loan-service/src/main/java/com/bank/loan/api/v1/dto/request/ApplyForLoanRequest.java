package com.bank.loan.api.v1.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigInteger;

public record ApplyForLoanRequest(

        @NotBlank
        String loanType,

        @NotBlank
        String disbursementAccountNumber,

        @NotNull
        @Positive
        BigInteger principalMinorUnits,

        @NotNull
        @Min(1)
        Integer tenureMonths,

        @NotBlank
        String currency
) {
}
