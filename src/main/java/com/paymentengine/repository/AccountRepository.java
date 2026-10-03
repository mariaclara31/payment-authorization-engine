package com.paymentengine.repository;

import com.paymentengine.domain.model.Account;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends CrudRepository<Account, UUID> {

    Optional<Account> findByCustomerId(String customerId);
}
