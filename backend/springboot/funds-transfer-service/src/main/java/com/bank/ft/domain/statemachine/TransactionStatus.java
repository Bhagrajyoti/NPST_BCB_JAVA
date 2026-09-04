package com.bank.ft.domain.statemachine;

public enum TransactionStatus {
    INITIATED,
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    REVERSED
}
