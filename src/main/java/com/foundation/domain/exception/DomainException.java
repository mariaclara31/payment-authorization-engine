package com.foundation.domain.exception;

public abstract class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
