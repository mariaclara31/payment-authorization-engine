package com.paymentengine.service;

import com.paymentengine.domain.exception.AccountNotFoundException;
import com.paymentengine.domain.exception.InsufficientBalanceException;
import com.paymentengine.domain.model.Account;
import com.paymentengine.domain.model.Transaction;
import com.paymentengine.repository.AccountRepository;
import com.paymentengine.repository.TransactionRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
    }

    @Transactional
    public Account openAccount(String customerId) {
        Objects.requireNonNull(customerId);
        Account account = new Account(customerId);
        Account saved = accountRepository.save(account);
        account.markAsPersisted();
        return saved;
    }

    @Transactional
    public Transaction deposit(UUID accountId, BigDecimal amount) {
        Account account = requireAccount(accountId);
        Transaction transaction = account.deposit(amount);
        accountRepository.save(account);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction withdraw(UUID accountId, BigDecimal amount) {
        Account account = requireAccount(accountId);
        Transaction transaction = account.withdraw(amount);
        accountRepository.save(account);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public void transfer(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount) {
        Objects.requireNonNull(sourceAccountId);
        Objects.requireNonNull(targetAccountId);
        Objects.requireNonNull(amount);

        if (sourceAccountId.equals(targetAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account source = requireAccount(sourceAccountId);
        Account target = requireAccount(targetAccountId);

        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        Transaction withdrawal = source.withdraw(amount);
        Transaction deposit = target.deposit(amount);

        accountRepository.save(source);
        accountRepository.save(target);
        transactionRepository.save(withdrawal);
        transactionRepository.save(deposit);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID accountId) {
        return requireAccount(accountId).getBalance();
    }

    private Account requireAccount(UUID accountId) {
        Objects.requireNonNull(accountId);
        return accountRepository.findById(accountId)
                .orElseThrow(() -> AccountNotFoundException.forId(accountId));
    }
}