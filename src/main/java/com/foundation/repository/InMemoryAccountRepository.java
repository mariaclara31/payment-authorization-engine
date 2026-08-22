package com.foundation.repository;

import com.foundation.domain.model.Account;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAccountRepository implements AccountRepository{

    private final Map<UUID, Account> store = new ConcurrentHashMap<>();

    @Override
    public Account save(Account account) {
        Objects.requireNonNull(account);
        store.put(account.getId(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        Objects.requireNonNull(id);
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Account> findByCustomerId(String customerId) {
        Objects.requireNonNull(customerId);
        return store.values().stream()
                .filter(a -> a.getCustomerId().equals(customerId))
                .findFirst();
    }

    @Override
    public List<Account> findAll(){
        return List.copyOf(store.values());
    }

    @Override
    public boolean existsById(UUID id) {
        Objects.requireNonNull(id);
        return store.containsKey(id);
    }

    @Override
    public void delete(UUID id) {
        Objects.requireNonNull(id);
        store.remove(id);
    }
}
