package com.paymentengine.service;

import com.paymentengine.domain.exception.AccountNotFoundException;
import com.paymentengine.domain.exception.InsufficientBalanceException;
import com.paymentengine.domain.model.Account;
import com.paymentengine.domain.model.Transaction;
import com.paymentengine.domain.model.TransactionType;
import com.paymentengine.repository.AccountRepository;
import com.paymentengine.repository.TransactionRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void constructor_shouldRejectNullAccountRepository() {
        assertThrows(NullPointerException.class,
                () -> new TransactionService(null, transactionRepository, transactionManager));
    }

    @Test
    void constructor_shouldRejectNullTransactionRepository() {
        assertThrows(NullPointerException.class,
                () -> new TransactionService(accountRepository, null, transactionManager));
    }

    @Test
    void constructor_shouldRejectNullTransactionManager() {
        assertThrows(NullPointerException.class,
                () -> new TransactionService(accountRepository, transactionRepository, null));
    }

    @Nested
    class OpenAccount {

        @Test
        void openAccount_shouldCreateAndPersistAccount() {
            when(accountRepository.save(any(Account.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Account account = transactionService.openAccount("customer-1");

            assertEquals("customer-1", account.getCustomerId());
            assertEquals(BigDecimal.ZERO, account.getBalance());
            verify(accountRepository).save(account);
        }

        @Test
        void openAccount_shouldRejectNullCustomerId() {
            assertThrows(NullPointerException.class,
                    () -> transactionService.openAccount(null));
            verifyNoInteractions(accountRepository);
        }
    }

    @Nested
    class Deposit {

        @Test
        void deposit_shouldUpdateBalanceAndPersistAccountAndTransaction() {
            Account account = new Account("customer-1");
            when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Transaction transaction = transactionService.deposit(account.getId(), new BigDecimal("100.00"));

            assertEquals(new BigDecimal("100.00"), account.getBalance());
            assertEquals(TransactionType.DEPOSIT, transaction.getType());
            verify(accountRepository).save(account);
            verify(transactionRepository).save(transaction);
        }

        @Test
        void deposit_shouldThrowWhenAccountNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(accountRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class,
                    () -> transactionService.deposit(unknownId, new BigDecimal("100.00")));
            verify(accountRepository, never()).save(any());
            verifyNoInteractions(transactionRepository);
        }

        @Test
        void deposit_shouldRejectNullAccountId() {
            assertThrows(NullPointerException.class,
                    () -> transactionService.deposit(null, new BigDecimal("100.00")));
        }
    }

    @Nested
    class Withdraw {

        @Test
        void withdraw_shouldUpdateBalanceAndPersistAccountAndTransaction() {
            Account account = new Account("customer-1");
            account.deposit(new BigDecimal("100.00"));
            when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Transaction transaction = transactionService.withdraw(account.getId(), new BigDecimal("30.00"));

            assertEquals(new BigDecimal("70.00"), account.getBalance());
            assertEquals(TransactionType.WITHDRAWAL, transaction.getType());
            verify(accountRepository).save(account);
            verify(transactionRepository).save(transaction);
        }

        @Test
        void withdraw_shouldThrowWhenBalanceInsufficient() {
            Account account = new Account("customer-1");
            account.deposit(new BigDecimal("20.00"));
            when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

            assertThrows(InsufficientBalanceException.class,
                    () -> transactionService.withdraw(account.getId(), new BigDecimal("50.00")));
            verify(accountRepository, never()).save(any());
            verifyNoInteractions(transactionRepository);
        }

        @Test
        void withdraw_shouldThrowWhenAccountNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(accountRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class,
                    () -> transactionService.withdraw(unknownId, new BigDecimal("10.00")));
        }
    }

    @Nested
    class Transfer {

        @Test
        void transfer_shouldMoveAmountBetweenAccountsAndPersistBothTransactions() {
            Account source = new Account("customer-source");
            source.deposit(new BigDecimal("200.00"));
            Account target = new Account("customer-target");

            when(accountRepository.findById(source.getId())).thenReturn(Optional.of(source));
            when(accountRepository.findById(target.getId())).thenReturn(Optional.of(target));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            transactionService.transfer(source.getId(), target.getId(), new BigDecimal("50.00"));

            assertEquals(new BigDecimal("150.00"), source.getBalance());
            assertEquals(new BigDecimal("50.00"), target.getBalance());
            verify(accountRepository).save(source);
            verify(accountRepository).save(target);
            verify(transactionRepository, times(2)).save(any(Transaction.class));
        }

        @Test
        void transfer_shouldNotModifyAnyAccountWhenBalanceInsufficient() {
            Account source = new Account("customer-source");
            source.deposit(new BigDecimal("30.00"));
            Account target = new Account("customer-target");

            when(accountRepository.findById(source.getId())).thenReturn(Optional.of(source));
            when(accountRepository.findById(target.getId())).thenReturn(Optional.of(target));

            assertThrows(InsufficientBalanceException.class,
                    () -> transactionService.transfer(source.getId(), target.getId(), new BigDecimal("100.00")));

            assertEquals(new BigDecimal("30.00"), source.getBalance());
            assertEquals(BigDecimal.ZERO, target.getBalance());
            verify(accountRepository, never()).save(any());
            verifyNoInteractions(transactionRepository);
        }

        @Test
        void transfer_shouldRejectTransferToSameAccount() {
            UUID accountId = UUID.randomUUID();

            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.transfer(accountId, accountId, new BigDecimal("10.00")));
            verifyNoInteractions(accountRepository);
            verifyNoInteractions(transactionRepository);
        }

        @Test
        void transfer_shouldThrowWhenSourceAccountNotFound() {
            UUID sourceId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            when(accountRepository.findById(sourceId)).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class,
                    () -> transactionService.transfer(sourceId, targetId, new BigDecimal("10.00")));
            verify(accountRepository, never()).save(any());
        }

        @Test
        void transfer_shouldThrowWhenTargetAccountNotFound() {
            Account source = new Account("customer-source");
            source.deposit(new BigDecimal("100.00"));
            UUID targetId = UUID.randomUUID();

            when(accountRepository.findById(source.getId())).thenReturn(Optional.of(source));
            when(accountRepository.findById(targetId)).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class,
                    () -> transactionService.transfer(source.getId(), targetId, new BigDecimal("10.00")));
            verify(accountRepository, never()).save(any());
            assertEquals(new BigDecimal("100.00"), source.getBalance());
        }
    }

    @Nested
    class GetBalance {

        @Test
        void getBalance_shouldReturnCurrentBalance() {
            Account account = new Account("customer-1");
            account.deposit(new BigDecimal("75.00"));
            when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

            assertEquals(new BigDecimal("75.00"), transactionService.getBalance(account.getId()));
        }

        @Test
        void getBalance_shouldThrowWhenAccountNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(accountRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class,
                    () -> transactionService.getBalance(unknownId));
        }
    }
}