package com.bank.termdeposit.common.constant;

/**
 * Single source of truth for API error codes. GlobalExceptionHandler maps
 * every exception type to one of these — never a raw exception message
 * leaked to a client.
 */
public final class ErrorCodeCatalog {

    private ErrorCodeCatalog() {
    }

    public static final String VALIDATION_FAILED = "TD-400-001";
    public static final String RESOURCE_NOT_FOUND = "TD-404-001";
    public static final String ILLEGAL_TRANSITION = "TD-409-001";
    public static final String IDEMPOTENCY_KEY_REUSED = "TD-409-002";
    public static final String IDOR_DENIED = "TD-403-001";
    public static final String CHECKSUM_MISMATCH = "TD-403-002";
    public static final String CBS_UNAVAILABLE = "TD-502-001";
    public static final String INTERNAL_ERROR = "TD-500-001";
}
