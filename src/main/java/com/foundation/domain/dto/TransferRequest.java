package com.foundation.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for transferring an amount between two accounts.
 */
public record TransferRequest(
        @NotNull(message = "sourceAccountId must not be null")
        UUID sourceAccountId,

        @NotNull(message = "targetAccountId must not be null")
        UUID targetAccountId,

        @NotNull(message = "amount must not be null")
        @Positive(message = "amount must be greater than zero")
        BigDecimal amount
) {
}
