package com.bank.loan.common.exception;

public class CbsUnavailableException extends RuntimeException {

    public CbsUnavailableException(String message) {
        super(message);
    }

    public CbsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
