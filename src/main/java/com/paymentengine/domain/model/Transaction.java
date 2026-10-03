package com.paymentengine.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Table("transactions")
public final class Transaction implements Persistable<UUID> {

    @Id
    private final UUID id;
    private final UUID accountId;
    private final BigDecimal amount;
    private final TransactionType type;
    private final TransactionStatus status;
    private final LocalDateTime createdAt;

    @Transient
    private final boolean isNew;

    public Transaction(
            UUID accountId,
            BigDecimal amount,
            TransactionType type,
            TransactionStatus status
    ) {
        this(UUID.randomUUID(), accountId, amount, type, status, LocalDateTime.now(), true);
    }

    @PersistenceCreator
    public Transaction(
            UUID id,
            UUID accountId,
            BigDecimal amount,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime createdAt
    ) {
        this(id, accountId, amount, type, status, createdAt, false);
    }

    private Transaction(
            UUID id,
            UUID accountId,
            BigDecimal amount,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime createdAt,
            boolean isNew
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.amount = validateAmount(amount);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.isNew = isNew;
    }

    private static BigDecimal validateAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        return amount;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}