package com.paymentengine.domain.model;

import com.paymentengine.domain.exception.InsufficientBalanceException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Table("accounts")
public class Account implements Persistable<UUID> {

    @Id
    private final UUID id;
    private final String customerId;
    private BigDecimal balance;

    @Version
    private Long version;

    @Transient
    private boolean isNew = true;

    public Account(String customerId) {
        this.id = UUID.randomUUID();
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.balance = BigDecimal.ZERO;
    }

    @PersistenceCreator
    public Account(UUID id, String customerId, BigDecimal balance, Long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.balance = Objects.requireNonNull(balance, "balance must not be null");
        this.version = version;
        this.isNew = false;
    }

    public Transaction deposit(BigDecimal amount) {
        validateAmount(amount);
        balance = balance.add(amount);
        return new Transaction(
                id,
                amount,
                TransactionType.DEPOSIT,
                TransactionStatus.APPROVED
        );
    }

    public Transaction withdraw(BigDecimal amount) {
        validateAmount(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("insufficient balance");
        }
        balance = balance.subtract(amount);
        return new Transaction(
                id,
                amount,
                TransactionType.WITHDRAWAL,
                TransactionStatus.APPROVED
        );
    }

    private void validateAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void markAsPersisted() {
        this.isNew = false;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Long getVersion() {
        return version;
    }
}