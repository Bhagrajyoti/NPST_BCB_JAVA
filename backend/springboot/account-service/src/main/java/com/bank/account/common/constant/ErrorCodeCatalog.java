package com.bank.account.common.constant;

/**
 * Single source of truth for API error codes. GlobalExceptionHandler maps
 * every exception type to one of these — never a raw exception message
 * leaked to a client.
 */
public final class ErrorCodeCatalog {

    private ErrorCodeCatalog() {
    }

    public static final String VALIDATION_FAILED = "ACC-400-001";
    public static final String RESOURCE_NOT_FOUND = "ACC-404-001";
    public static final String ILLEGAL_TRANSITION = "ACC-409-001";
    public static final String IDEMPOTENCY_KEY_REUSED = "ACC-409-002";
    public static final String IDOR_DENIED = "ACC-403-001";
    public static final String CHECKSUM_MISMATCH = "ACC-403-002";
    public static final String CBS_UNAVAILABLE = "ACC-502-001";
    public static final String INTERNAL_ERROR = "ACC-500-001";
}
