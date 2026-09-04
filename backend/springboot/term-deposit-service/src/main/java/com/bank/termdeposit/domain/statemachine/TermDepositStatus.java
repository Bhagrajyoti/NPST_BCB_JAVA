package com.bank.termdeposit.domain.statemachine;

public enum TermDepositStatus {
    PENDING,
    ACTIVE,
    MATURED,
    PREMATURELY_CLOSED,
    CLOSED
}
