package com.foundation.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class Transaction {
    private final UUID id;
    private final String customerId;
    private final BigDecimal amount;
    private final TransactionType type;
    private final TransactionStatus status;
    private final LocalDateTime createdAt;

    
    public Transaction(
            String customerId,
            BigDecimal amount,
            TransactionType type,
            TransactionStatus status
    ) {
        this(UUID.randomUUID(), customerId, amount, type, status, LocalDateTime.now());
    }

    
    public Transaction(
            UUID id,
            String customerId,
            BigDecimal amount,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.amount = validateAmount(amount);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static BigDecimal validateAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        return amount;
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
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

