package com.foundation.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InsufficientBalanceExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        String message = "insufficient balance";
        InsufficientBalanceException exception = new InsufficientBalanceException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void shouldExtendRuntimeException() {
        InsufficientBalanceException exception = new InsufficientBalanceException("test");

        assertTrue(exception instanceof RuntimeException);
    }
}
