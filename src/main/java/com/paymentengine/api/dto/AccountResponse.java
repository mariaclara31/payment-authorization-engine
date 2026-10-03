package com.paymentengine.api.dto;

import com.paymentengine.domain.model.Account;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response representing an account's public state.
 *
 * <p>Deliberately exposes only id, customerId and balance — not the full
 * transaction list — keeping the API contract decoupled from the internal
 * structure of the {@link Account} aggregate.
 */
public record AccountResponse(
        UUID id,
        String customerId,
        BigDecimal balance
) {
    /**
     * Maps a domain {@link Account} to its API representation.
     */
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getBalance()
        );
    }
}
