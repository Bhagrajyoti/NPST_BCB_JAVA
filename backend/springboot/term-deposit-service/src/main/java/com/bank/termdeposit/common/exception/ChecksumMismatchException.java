package com.bank.termdeposit.common.exception;

public class ChecksumMismatchException extends RuntimeException {

    public ChecksumMismatchException(String message) {
        super(message);
    }
}
