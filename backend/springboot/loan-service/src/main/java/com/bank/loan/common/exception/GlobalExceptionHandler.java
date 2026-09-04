package com.bank.loan.common.exception;

import com.bank.loan.common.constant.ErrorCodeCatalog;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The only place exceptions become HTTP responses. Every response body uses
 * the same {@link ErrorResponse} shape and a code from
 * {@link ErrorCodeCatalog} — never a raw stack trace or exception message.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, ErrorCodeCatalog.RESOURCE_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalTransitionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalTransition(IllegalTransitionException ex) {
        return respond(HttpStatus.CONFLICT, ErrorCodeCatalog.ILLEGAL_TRANSITION, ex.getMessage());
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyReuse(IdempotencyKeyReusedException ex) {
        return respond(HttpStatus.CONFLICT, ErrorCodeCatalog.IDEMPOTENCY_KEY_REUSED, ex.getMessage());
    }

    @ExceptionHandler(IdorDeniedException.class)
    public ResponseEntity<ErrorResponse> handleIdor(IdorDeniedException ex) {
        return respond(HttpStatus.FORBIDDEN, ErrorCodeCatalog.IDOR_DENIED, ex.getMessage());
    }

    @ExceptionHandler(ChecksumMismatchException.class)
    public ResponseEntity<ErrorResponse> handleChecksumMismatch(ChecksumMismatchException ex) {
        return respond(HttpStatus.FORBIDDEN, ErrorCodeCatalog.CHECKSUM_MISMATCH, ex.getMessage());
    }

    @ExceptionHandler(CbsUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleCbsUnavailable(CbsUnavailableException ex) {
        return respond(HttpStatus.BAD_GATEWAY, ErrorCodeCatalog.CBS_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, ErrorCodeCatalog.VALIDATION_FAILED, detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCodeCatalog.VALIDATION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodeCatalog.INTERNAL_ERROR, "Unexpected error");
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, Instant.now()));
    }

    public record ErrorResponse(String code, String message, Instant timestamp) {
    }
}
