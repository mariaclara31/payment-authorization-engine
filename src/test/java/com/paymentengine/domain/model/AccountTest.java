package com.paymentengine.domain.model;

import com.paymentengine.domain.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void shouldCreateAccountWithZeroBalance() {
        Account account = new Account("customer-1");

        assertEquals("customer-1", account.getCustomerId());
        assertEquals(BigDecimal.ZERO, account.getBalance());
        assertNotNull(account.getId());
    }

    @Test
    void shouldDepositAmountAndReturnTransaction() {
        Account account = new Account("customer-1");

        Transaction transaction = account.deposit(new BigDecimal("100.00"));

        assertEquals(new BigDecimal("100.00"), account.getBalance());
        assertEquals(TransactionType.DEPOSIT, transaction.getType());
        assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals(account.getId(), transaction.getAccountId());
    }

    @Test
    void shouldWithdrawAmountAndReturnTransaction() {
        Account account = new Account("customer-1");
        account.deposit(new BigDecimal("100.00"));

        Transaction transaction = account.withdraw(new BigDecimal("30.00"));

        assertEquals(new BigDecimal("70.00"), account.getBalance());
        assertEquals(TransactionType.WITHDRAWAL, transaction.getType());
        assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        assertEquals(new BigDecimal("30.00"), transaction.getAmount());
        assertEquals(account.getId(), transaction.getAccountId());
    }

    @Test
    void shouldNotWithdrawWhenBalanceIsInsufficient() {
        Account account = new Account("customer-1");
        account.deposit(new BigDecimal("50.00"));

        InsufficientBalanceException exception = assertThrows(
                InsufficientBalanceException.class,
                () -> account.withdraw(new BigDecimal("80.00"))
        );

        assertEquals("insufficient balance", exception.getMessage());
        assertEquals(new BigDecimal("50.00"), account.getBalance());
    }

    @ParameterizedTest
    @MethodSource("invalidTransactionOperations")
    void shouldRejectInvalidAmount(Executable operation) {
        assertThrows(IllegalArgumentException.class, operation);
    }

    private static Stream<Executable> invalidTransactionOperations() {
        return Stream.of(
                () -> new Account("customer-1").deposit(BigDecimal.ZERO),
                () -> new Account("customer-1").deposit(new BigDecimal("-10.00")),
                () -> new Account("customer-1").withdraw(BigDecimal.ZERO),
                () -> new Account("customer-1").withdraw(new BigDecimal("-10.00"))
        );
    }
}