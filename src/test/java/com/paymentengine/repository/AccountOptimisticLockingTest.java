package com.paymentengine.repository;

import com.paymentengine.domain.model.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJdbcTest
class AccountOptimisticLockingTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("version is assigned on insert and incremented by one on each update")
    void versionIncrementsOnEachUpdate() {
        Account saved = accountRepository.save(new Account("version-customer"));

        Account firstLoad = accountRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(firstLoad.getVersion(), "version must be loaded from the database");
        Long versionAfterInsert = firstLoad.getVersion();

        firstLoad.deposit(new BigDecimal("10.00"));
        accountRepository.save(firstLoad);

        Account secondLoad = accountRepository.findById(saved.getId()).orElseThrow();
        assertEquals(versionAfterInsert + 1, secondLoad.getVersion(),
                "version must increase by exactly one after one update");

        secondLoad.deposit(new BigDecimal("10.00"));
        accountRepository.save(secondLoad);

        Account thirdLoad = accountRepository.findById(saved.getId()).orElseThrow();
        assertEquals(versionAfterInsert + 2, thirdLoad.getVersion(),
                "version must increase by exactly one after each update");
    }

    @Test
    @DisplayName("saving a stale copy of an account is rejected with an optimistic locking failure")
    void staleWriteIsRejected() {
        Account saved = accountRepository.save(new Account("stale-customer"));

        Account first = accountRepository.findById(saved.getId()).orElseThrow();
        Account second = accountRepository.findById(saved.getId()).orElseThrow();

        first.deposit(new BigDecimal("10.00"));
        accountRepository.save(first);

        second.deposit(new BigDecimal("20.00"));
        assertThrows(OptimisticLockingFailureException.class,
                () -> accountRepository.save(second));
    }
}