package com.paymentengine.repository;

import com.paymentengine.domain.model.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends CrudRepository<Transaction, UUID> {

    List<Transaction> findByAccountId(UUID accountId);
}
