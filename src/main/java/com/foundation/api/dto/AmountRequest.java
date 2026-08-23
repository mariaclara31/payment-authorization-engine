package com.foundation.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request body carrying a monetary amount, used for deposits and withdrawals.
 */
public record AmountRequest(
        @NotNull(message = "amount must not be null")
        @Positive(message = "amount must be greater than zero")
        BigDecimal amount
) {
}
