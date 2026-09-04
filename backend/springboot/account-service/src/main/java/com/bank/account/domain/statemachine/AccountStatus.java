package com.bank.account.domain.statemachine;

public enum AccountStatus {
    PENDING_ACTIVATION,
    ACTIVE,
    DORMANT,
    FROZEN,
    CLOSED
}
