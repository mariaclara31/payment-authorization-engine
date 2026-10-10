package com.paymentengine.service;

import com.paymentengine.domain.exception.AccountNotFoundException;
import com.paymentengine.domain.exception.InsufficientBalanceException;
import com.paymentengine.domain.model.Account;
import com.paymentengine.domain.model.Transaction;
import com.paymentengine.repository.AccountRepository;
import com.paymentengine.repository.TransactionRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class TransactionService {

    private static final int MAX_OPTIMISTIC_RETRIES = 20;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionTemplate transactionTemplate;

    public TransactionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.transactionTemplate = new TransactionTemplate(Objects.requireNonNull(transactionManager));
    }

    public Account openAccount(String customerId) {
        Objects.requireNonNull(customerId);
        return transactionTemplate.execute(status -> {
            Account account = new Account(customerId);
            Account saved = accountRepository.save(account);
            account.markAsPersisted();
            return saved;
        });
    }

    public Transaction deposit(UUID accountId, BigDecimal amount) {
        return executeInTransactionWithRetry(status -> {
            Account account = requireAccount(accountId);
            Transaction transaction = account.deposit(amount);
            accountRepository.save(account);
            return transactionRepository.save(transaction);
        });
    }

    public Transaction withdraw(UUID accountId, BigDecimal amount) {
        return executeInTransactionWithRetry(status -> {
            Account account = requireAccount(accountId);
            Transaction transaction = account.withdraw(amount);
            accountRepository.save(account);
            return transactionRepository.save(transaction);
        });
    }

    public void transfer(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount) {
        Objects.requireNonNull(sourceAccountId);
        Objects.requireNonNull(targetAccountId);
        Objects.requireNonNull(amount);

        if (sourceAccountId.equals(targetAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        executeInTransactionWithRetry(status -> {
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

            return null;
        });
    }

    public BigDecimal getBalance(UUID accountId) {
        return requireAccount(accountId).getBalance();
    }

    private Account requireAccount(UUID accountId) {
        Objects.requireNonNull(accountId);
        return accountRepository.findById(accountId)
                .orElseThrow(() -> AccountNotFoundException.forId(accountId));
    }

    private <T> T executeInTransactionWithRetry(TransactionCallback<T> action) {
        OptimisticLockingFailureException lastFailure = null;
        for (int attempt = 0; attempt < MAX_OPTIMISTIC_RETRIES; attempt++) {
            try {
                return transactionTemplate.execute(action);
            } catch (OptimisticLockingFailureException e) {
                lastFailure = e;
            }
        }
        throw lastFailure;
    }
}