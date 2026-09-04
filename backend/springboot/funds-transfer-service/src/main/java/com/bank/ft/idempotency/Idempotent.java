package com.bank.ft.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method whose write must be safe under client retry.
 * The method must return {@code ResponseEntity<?>}; see
 * {@link IdempotencyAspect}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Idempotent {

    /** Hours before the stored record expires and can be purged/reused. */
    int ttlHours() default 24;
}
