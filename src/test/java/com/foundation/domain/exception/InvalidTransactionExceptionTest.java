package com.foundation.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidTransactionExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        String message = "invalid transaction";
        InvalidTransactionException exception = new InvalidTransactionException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void shouldExtendRuntimeException() {
        InvalidTransactionException exception = new InvalidTransactionException("test");

        assertTrue(exception instanceof RuntimeException);
    }
}
