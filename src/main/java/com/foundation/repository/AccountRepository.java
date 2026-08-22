package com.foundation.repository;

import com.foundation.domain.model.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Account save(Account account);
    Optional<Account> findById(UUID id);
    Optional<Account> findByCustomerId(String customerId);
    List<Account> findAll();
    boolean existsById(UUID id);
    void delete(UUID id);
}
