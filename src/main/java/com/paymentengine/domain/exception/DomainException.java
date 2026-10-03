package com.paymentengine.domain.exception;

public abstract class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
