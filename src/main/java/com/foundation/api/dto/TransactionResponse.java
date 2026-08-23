package com.foundation.api.dto;

import com.foundation.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response representing a single transaction.
 */
public record TransactionResponse(
        UUID id,
        String customerId,
        BigDecimal amount,
        String type,
        String status,
        LocalDateTime createdAt
) {
    /**
     * Maps a domain {@link Transaction} to its API representation.
     */
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCustomerId(),
                transaction.getAmount(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.getCreatedAt()
        );
    }
}
