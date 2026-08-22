package com.foundation.domain.model;

import com.foundation.domain.exception.InsufficientBalanceException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Account {
    private final UUID id;
    private final String customerId;
    private BigDecimal balance;
    private final List<Transaction> transactions;

    public Account(String customerId) {
        this.id = UUID.randomUUID();
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.balance = BigDecimal.ZERO;
        this.transactions = new ArrayList<>();
    }

    public Transaction deposit(BigDecimal amount) {
        validateAmount(amount);

        balance = balance.add(amount);

        Transaction transaction = new Transaction(
                customerId,
                amount,
                TransactionType.DEPOSIT,
                TransactionStatus.APPROVED
        );

        transactions.add(transaction);

        return transaction;
    }

    public Transaction withdraw(BigDecimal amount) {
        validateAmount(amount);

        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("insufficient balance");
        }

        balance = balance.subtract(amount);

        Transaction transaction = new Transaction(
                customerId,
                amount,
                TransactionType.WITHDRAWAL,
                TransactionStatus.APPROVED
        );

        transactions.add(transaction);

        return transaction;
    }

    private void validateAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public List<Transaction> getTransactions() {
        return List.copyOf(transactions);
    }
}
