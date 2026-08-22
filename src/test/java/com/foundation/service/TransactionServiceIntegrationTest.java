package com.foundation.service;

import com.foundation.domain.exception.AccountNotFoundException;
import com.foundation.domain.exception.InsufficientBalanceException;
import com.foundation.domain.model.Account;
import com.foundation.repository.AccountRepository;
import com.foundation.repository.InMemoryAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionServiceIntegrationTest {

    private AccountRepository repository;
    private TransactionService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAccountRepository();
        service = new TransactionService(repository);
    }

    @Nested
    class AccountLifecycle {

        @Test
        @DisplayName("opened account is persisted and retrievable from the repository")
        void openedAccountIsPersisted() {
            Account account = service.openAccount("customer-1");

            assertTrue(repository.findById(account.getId()).isPresent());
            assertBalance(account.getId(), "0");
        }
    }

    @Nested
    class DepositAndWithdraw {

        @Test
        @DisplayName("balance reflects a sequence of deposits and withdrawals")
        void balanceReflectsOperationSequence() {
            UUID id = openAccountWith("customer-1", "100.00");
            service.deposit(id, money("50.00"));
            service.withdraw(id, money("30.00"));

            assertBalance(id, "120.00");
        }

        @Test
        @DisplayName("withdrawing more than the balance leaves the persisted state unchanged")
        void insufficientWithdrawalDoesNotChangePersistedState() {
            UUID id = openAccountWith("customer-1", "40.00");

            assertThrows(InsufficientBalanceException.class,
                    () -> service.withdraw(id, money("100.00")));

            assertBalance(id, "40.00");
        }
    }

    @Nested
    class Transfer {

        @Test
        @DisplayName("transfer moves funds and persists both accounts")
        void transferMovesFundsAndPersistsBothAccounts() {
            UUID source = openAccountWith("customer-source", "200.00");
            UUID target = openAccountWith("customer-target", "0");

            service.transfer(source, target, money("75.00"));

            assertBalance(source, "125.00");
            assertBalance(target, "75.00");
        }

        @Test
        @DisplayName("failed transfer (insufficient funds) leaves both balances intact")
        void failedTransferLeavesBothBalancesIntact() {
            UUID source = openAccountWith("customer-source", "50.00");
            UUID target = openAccountWith("customer-target", "10.00");

            assertThrows(InsufficientBalanceException.class,
                    () -> service.transfer(source, target, money("500.00")));

            assertBalance(source, "50.00");
            assertBalance(target, "10.00");
        }

        @Test
        @DisplayName("transfer to a non-existent target leaves the source untouched")
        void transferToMissingTargetLeavesSourceUntouched() {
            UUID source = openAccountWith("customer-source", "100.00");
            UUID missingTarget = UUID.randomUUID();

            assertThrows(AccountNotFoundException.class,
                    () -> service.transfer(source, missingTarget, money("10.00")));

            assertBalance(source, "100.00");
        }
    }

    @Nested
    class FullScenario {

        @Test
        @DisplayName("realistic multi-account, multi-operation scenario settles to correct balances")
        void realisticScenarioSettlesCorrectly() {
            UUID alice = openAccountWith("alice", "1000.00");
            UUID bob   = openAccountWith("bob", "500.00");
            UUID carol = openAccountWith("carol", "0");

            service.transfer(alice, bob, money("200.00"));   // alice 800, bob 700
            service.transfer(bob, carol, money("300.00"));   // bob 400, carol 300
            service.withdraw(alice, money("100.00"));        // alice 700

            assertBalance(alice, "700.00");
            assertBalance(bob, "400.00");
            assertBalance(carol, "300.00");

            BigDecimal total = service.getBalance(alice)
                    .add(service.getBalance(bob))
                    .add(service.getBalance(carol));
            assertEquals(money("1400.00"), total);
        }
    }

    private UUID openAccountWith(String customerId, String initialAmount) {
        Account account = service.openAccount(customerId);
        BigDecimal amount = money(initialAmount);
        if (amount.signum() > 0) {
            service.deposit(account.getId(), amount);
        }
        return account.getId();
    }

    private void assertBalance(UUID accountId, String expected) {
        assertEquals(money(expected), service.getBalance(accountId));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
