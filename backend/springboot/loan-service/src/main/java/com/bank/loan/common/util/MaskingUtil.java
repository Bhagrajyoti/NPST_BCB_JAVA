package com.bank.loan.common.util;

/**
 * Masking is enforced centrally (logback JSON encoder + this util), not
 * opt-in per log statement — PII/PCI fields must never appear unmasked in
 * application logs.
 */
public final class MaskingUtil {

    private MaskingUtil() {
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return "*".repeat(accountNumber.length() - 4) + accountNumber.substring(accountNumber.length() - 4);
    }

    public static String maskCif(String cif) {
        if (cif == null || cif.length() <= 2) {
            return "**";
        }
        return cif.substring(0, 2) + "*".repeat(cif.length() - 2);
    }
}
