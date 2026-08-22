package com.foundation.domain.exception;

import java.util.UUID;

public class AccountNotFoundException extends DomainException {

    public AccountNotFoundException(String message) {
        super(message);
    }

    public static AccountNotFoundException forId(UUID id) {
        return new AccountNotFoundException(String.format("Account with id %s not found", id));
    }

    public static AccountNotFoundException forCustomerId(String customerId) {
        return new AccountNotFoundException(String.format("Customer with id %s not found", customerId));
    }
}
