package com.bank.account.idempotency;

public enum IdempotencyStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
