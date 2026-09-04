package com.bank.ft.api.v1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateBeneficiaryRequest(

        @NotBlank
        String beneficiaryName,

        @NotBlank
        String beneficiaryAccountNumber,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "beneficiaryIfscCode must be a valid IFSC code")
        String beneficiaryIfscCode,

        String nickname,

        @NotBlank
        String transferMode
) {
}
