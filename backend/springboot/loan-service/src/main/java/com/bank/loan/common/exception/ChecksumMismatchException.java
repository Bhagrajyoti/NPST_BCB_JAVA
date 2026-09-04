package com.bank.loan.common.exception;

public class ChecksumMismatchException extends RuntimeException {

    public ChecksumMismatchException(String message) {
        super(message);
    }
}
