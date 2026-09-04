package com.bank.account.messaging.outbox;

public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}
