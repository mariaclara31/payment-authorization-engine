package com.foundation.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final BigDecimal VALID_AMOUNT = BigDecimal.TEN;
    private static final TransactionType TYPE = TransactionType.DEPOSIT;
    private static final TransactionStatus STATUS = TransactionStatus.PENDING;

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-10.00"})
    void shouldNotAcceptInvalidAmount(String invalidAmount) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Transaction(
                        CUSTOMER_ID,
                        new BigDecimal(invalidAmount),
                        TYPE,
                        STATUS
                )
        );

        assertEquals("amount must be greater than zero", exception.getMessage());
    }

    @Test
    void shouldNotAcceptNullRequiredFields() {
        assertRequiredFieldNullThrows("customerId must not be null",
                () -> new Transaction(null, VALID_AMOUNT, TYPE, STATUS));

        assertRequiredFieldNullThrows("amount must not be null",
                () -> new Transaction(CUSTOMER_ID, null, TYPE, STATUS));

        assertRequiredFieldNullThrows("type must not be null",
                () -> new Transaction(CUSTOMER_ID, VALID_AMOUNT, null, STATUS));

        assertRequiredFieldNullThrows("status must not be null",
                () -> new Transaction(CUSTOMER_ID, VALID_AMOUNT, TYPE, null));

    }

    private static void assertRequiredFieldNullThrows(String expectedMessage, Executable executable) {
        NullPointerException exception = assertThrows(NullPointerException.class, executable);
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void shouldCreateTransactionWhenFieldsAreValid() {
        Transaction transaction = new Transaction(
                CUSTOMER_ID,
                VALID_AMOUNT,
                TYPE,
                STATUS
        );
        assertNotNull(transaction.getId());
        assertNotNull(transaction.getCreatedAt());
        assertEquals(CUSTOMER_ID, transaction.getCustomerId());
        assertEquals(VALID_AMOUNT, transaction.getAmount());
        assertEquals(TYPE, transaction.getType());
        assertEquals(STATUS, transaction.getStatus());

    }
}
