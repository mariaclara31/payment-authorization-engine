package com.paymentengine.service;

import com.paymentengine.domain.exception.InsufficientBalanceException;
import com.paymentengine.domain.model.Account;
import com.paymentengine.domain.model.Transaction;
import com.paymentengine.domain.model.TransactionType;
import com.paymentengine.repository.AccountRepository;
import com.paymentengine.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJdbcTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext
class TransactionServiceConcurrencyTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("concurrent withdrawals that would overdraw the account are safely rejected, others succeed via retry")
    void concurrentWithdrawalsNeverOverdrawTheAccount() throws InterruptedException {
        TransactionService service =
                new TransactionService(accountRepository, transactionRepository, transactionManager);

        Account account = service.openAccount("concurrency-test-customer");
        service.deposit(account.getId(), new BigDecimal("100.00"));
        UUID accountId = account.getId();

        int threadCount = 10;
        BigDecimal withdrawalAmount = new BigDecimal("20.00");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger insufficientBalanceCount = new AtomicInteger(0);
        AtomicInteger unexpectedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    service.withdraw(accountId, withdrawalAmount);
                    successCount.incrementAndGet();
                } catch (OptimisticLockingFailureException e) {
                    conflictCount.incrementAndGet();
                } catch (InsufficientBalanceException e) {
                    insufficientBalanceCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    unexpectedCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal finalBalance = service.getBalance(accountId);
        String summary = "success=" + successCount.get()
                + ", conflict=" + conflictCount.get()
                + ", insufficient=" + insufficientBalanceCount.get()
                + ", unexpected=" + unexpectedCount.get()
                + ", finalBalance=" + finalBalance;

        assertTrue(completed, "all threads should finish within the timeout. " + summary);
        assertEquals(0, unexpectedCount.get(),
                "no thread should fail with an unexpected exception type. " + summary);

        int totalAttempts = successCount.get() + conflictCount.get() + insufficientBalanceCount.get();
        assertEquals(threadCount, totalAttempts,
                "every thread must end in exactly one outcome. " + summary);

        assertEquals(5, successCount.get(),
                "exactly 5 withdrawals of 20.00 should succeed against a balance of 100.00. " + summary);

        BigDecimal expectedFinalBalance = new BigDecimal("100.00")
                .subtract(withdrawalAmount.multiply(BigDecimal.valueOf(successCount.get())));
        assertEquals(expectedFinalBalance, finalBalance, summary);

        List<Transaction> persistedTransactions = transactionRepository.findByAccountId(accountId);
        long withdrawalRecords = persistedTransactions.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                .count();
        assertEquals(successCount.get(), withdrawalRecords,
                "exactly one transaction record must exist per successful withdrawal. " + summary);
    }
}