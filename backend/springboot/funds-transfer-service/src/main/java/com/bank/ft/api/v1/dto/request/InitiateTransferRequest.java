package com.bank.ft.api.v1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigInteger;
import java.util.UUID;

public record InitiateTransferRequest(

        UUID beneficiaryId, // nullable — a one-time payee not saved as a Beneficiary is still valid

        @NotBlank
        String destinationAccountNumber,

        @NotBlank
        String destinationIfscCode,

        @NotNull
        @Positive
        BigInteger amountMinorUnits,

        @NotBlank
        String currency,

        @NotBlank
        String transferMode, // IMPS, NEFT, RTGS, INTRA_BANK

        String remarks
) {
}
