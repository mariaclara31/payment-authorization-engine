package com.foundation.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for opening a new account.
 */
public record OpenAccountRequest(
        @NotBlank(message = "customerId must not be blank")
        String customerId
) {
}
