package com.foundation.service;

import com.foundation.domain.exception.AccountNotFoundException;
import com.foundation.domain.exception.InsufficientBalanceException;
import com.foundation.domain.model.Account;
import com.foundation.domain.model.Transaction;
import com.foundation.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class TransactionService {

    private final AccountRepository accountRepository;

    public TransactionService(AccountRepository accountRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
    }

    public Account openAccount(String customerId) {
        Objects.requireNonNull(customerId);
        Account account = new Account(customerId);
        return accountRepository.save(account);
    }

    public Transaction deposit (UUID accountId, BigDecimal amount) {
        Account account = requireAccount(accountId);
        Transaction transaction = account.deposit(amount);
        accountRepository.save(account);
        return transaction;
    }

    public Transaction withdraw (UUID accountId, BigDecimal amount) {
        Account account = requireAccount(accountId);
        Transaction transaction = account.withdraw(amount);
        accountRepository.save(account);
        return transaction;
    }

    public void transfer (UUID sourceAccountId,  UUID targetAccountId, BigDecimal amount) {
        Objects.requireNonNull(sourceAccountId);
        Objects.requireNonNull(targetAccountId);
        Objects.requireNonNull(amount);

        if (sourceAccountId.equals(targetAccountId)) {
            throw  new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account source = requireAccount(sourceAccountId);
        Account target = requireAccount(targetAccountId);

        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        source.withdraw(amount);
        target.deposit(amount);

        accountRepository.save(source);
        accountRepository.save(target);
    }

    public BigDecimal getBalance(UUID accountId) {
        return requireAccount(accountId).getBalance();
    }

    private Account requireAccount(UUID accountId) {
        Objects.requireNonNull(accountId);
        return accountRepository.findById(accountId)
                .orElseThrow(() -> AccountNotFoundException.forId(accountId));
    }
}
