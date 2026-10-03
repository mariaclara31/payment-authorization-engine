package com.paymentengine.domain.exception;

public class InsufficientBalanceException extends DomainException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
