package com.bank.loan.domain.statemachine;

public enum LoanStatus {
    APPLIED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    DISBURSED,
    ACTIVE,
    CLOSED,
    FORECLOSED,
    DEFAULTED
}
