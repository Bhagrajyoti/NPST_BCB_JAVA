package com.bank.loan.common.constant;

/**
 * Single source of truth for API error codes. GlobalExceptionHandler maps
 * every exception type to one of these — never a raw exception message
 * leaked to a client.
 */
public final class ErrorCodeCatalog {

    private ErrorCodeCatalog() {
    }

    public static final String VALIDATION_FAILED = "LN-400-001";
    public static final String RESOURCE_NOT_FOUND = "LN-404-001";
    public static final String ILLEGAL_TRANSITION = "LN-409-001";
    public static final String IDEMPOTENCY_KEY_REUSED = "LN-409-002";
    public static final String IDOR_DENIED = "LN-403-001";
    public static final String CHECKSUM_MISMATCH = "LN-403-002";
    public static final String CBS_UNAVAILABLE = "LN-502-001";
    public static final String INTERNAL_ERROR = "LN-500-001";
}
