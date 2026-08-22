package com.foundation.repository;

import com.foundation.domain.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryAccountRepositoryTest {
    private InMemoryAccountRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAccountRepository();
    }

    @Nested
    class Save {

        @Test
        void save_shouldPersistAccountAndReturnIt() {
            Account account = new Account("customer-1");

            Account saved = repository.save(account);

            assertSame(account, saved);
            assertTrue(repository.existsById(account.getId()));
        }

        @Test
        void save_shouldOverwriteExistingAccountWithSameId() {
            Account account = new Account("customer-1");
            repository.save(account);

            account.deposit(new BigDecimal("100.00"));
            repository.save(account);

            Account found = repository.findById(account.getId()).orElseThrow();
            assertEquals(new BigDecimal("100.00"), found.getBalance());
        }

        @Test
        void save_shouldRejectNullAccount() {
            assertThrows(NullPointerException.class, () -> repository.save(null));
        }
    }

    @Nested
    class FindById {

        @Test
        void findById_shouldReturnAccountWhenExists() {
            Account account = new Account("customer-1");
            repository.save(account);

            Optional<Account> result = repository.findById(account.getId());

            assertTrue(result.isPresent());
            assertSame(account, result.get());
        }

        @Test
        void findById_shouldReturnEmptyWhenNotFound() {
            assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
        }

        @Test
        void findById_shouldRejectNullId() {
            assertThrows(NullPointerException.class, () -> repository.findById(null));
        }
    }

    @Nested
    class FindByCustomerId {

        @Test
        void findByCustomerId_shouldReturnAccountWhenExists() {
            Account account = new Account("customer-42");
            repository.save(account);

            Optional<Account> result = repository.findByCustomerId("customer-42");

            assertTrue(result.isPresent());
            assertSame(account, result.get());
        }

        @Test
        void findByCustomerId_shouldReturnEmptyWhenNotFound() {
            assertTrue(repository.findByCustomerId("customer-99").isEmpty());
        }

        @Test
        void findByCustomerId_shouldRejectNullCustomerId() {
            assertThrows(NullPointerException.class, () -> repository.findByCustomerId(null));
        }
    }

    @Nested
    class FindAll {

        @Test
        void findAll_shouldReturnAllSavedAccounts() {
            Account first  = new Account("customer-1");
            Account second = new Account("customer-2");
            repository.save(first);
            repository.save(second);

            List<Account> all = repository.findAll();

            assertEquals(2, all.size());
            assertTrue(all.containsAll(List.of(first, second)));
        }

        @Test
        void findAll_shouldReturnEmptyListWhenNoAccountsExist() {
            assertTrue(repository.findAll().isEmpty());
        }

        @Test
        void findAll_shouldReturnUnmodifiableList() {
            repository.save(new Account("customer-1"));

            List<Account> all = repository.findAll();

            assertThrows(UnsupportedOperationException.class,
                    () -> all.add(new Account("customer-2")));
        }
    }

    @Nested
    class ExistsById {

        @Test
        void existsById_shouldReturnTrueForSavedAccount() {
            Account account = new Account("customer-1");
            repository.save(account);

            assertTrue(repository.existsById(account.getId()));
        }

        @Test
        void existsById_shouldReturnFalseForUnknownId() {
            assertFalse(repository.existsById(UUID.randomUUID()));
        }

        @Test
        void existsById_shouldRejectNullId() {
            assertThrows(NullPointerException.class, () -> repository.existsById(null));
        }
    }

    @Nested
    class Delete {

        @Test
        void delete_shouldRemoveAccountById() {
            Account account = new Account("customer-1");
            repository.save(account);

            repository.delete(account.getId());

            assertFalse(repository.existsById(account.getId()));
        }

        @Test
        void delete_shouldDoNothingWhenAccountDoesNotExist() {
            // deve ser idempotente — não lança exceção
            repository.delete(UUID.randomUUID());
        }

        @Test
        void delete_shouldRejectNullId() {
            assertThrows(NullPointerException.class, () -> repository.delete(null));
        }
    }
}
