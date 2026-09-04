package com.bank.ft.common.constant;

/**
 * Single source of truth for API error codes. GlobalExceptionHandler maps
 * every exception type to one of these — never a raw exception message
 * leaked to a client.
 */
public final class ErrorCodeCatalog {

    private ErrorCodeCatalog() {
    }

    public static final String VALIDATION_FAILED = "FT-400-001";
    public static final String RESOURCE_NOT_FOUND = "FT-404-001";
    public static final String ILLEGAL_TRANSITION = "FT-409-001";
    public static final String IDEMPOTENCY_KEY_REUSED = "FT-409-002";
    public static final String IDOR_DENIED = "FT-403-001";
    public static final String CHECKSUM_MISMATCH = "FT-403-002";
    public static final String CBS_UNAVAILABLE = "FT-502-001";
    public static final String INTERNAL_ERROR = "FT-500-001";
}
