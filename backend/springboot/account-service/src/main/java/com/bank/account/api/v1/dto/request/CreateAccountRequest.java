package com.bank.account.api.v1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(

        @NotBlank
        String cif,

        @NotBlank
        @Pattern(regexp = "SAVINGS|CURRENT", message = "accountType must be SAVINGS or CURRENT")
        String accountType,

        @NotBlank
        @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter ISO code")
        String currency
) {
}
