package com.paymentengine.domain.model;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public final class TransactionCollection {

    private static final TransactionCollection EMPTY = new TransactionCollection(List.of());

    private final List<Transaction> items;

    private TransactionCollection(List<Transaction> items) {
        this.items = List.copyOf(items);
    }
    
    public static TransactionCollection of(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");
        if (transactions.isEmpty()) {
            return EMPTY;
        }
        return new TransactionCollection(transactions);
    }

    
    public static TransactionCollection empty() {
        return EMPTY;
    }


    public TransactionCollection filter(Predicate<Transaction> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");

        List<Transaction> filtered = items.stream()
                .filter(predicate)
                .toList();

        return filtered.isEmpty() ? EMPTY : new TransactionCollection(filtered);
    }

    
    public TransactionCollection withStatus(TransactionStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        return filter(t -> t.getStatus() == status);
    }

    
    public TransactionCollection approved() {
        return withStatus(TransactionStatus.APPROVED);
    }

    
    public TransactionCollection rejected() {
        return withStatus(TransactionStatus.REJECTED);
    }

    
    public TransactionCollection deduplicateById() {
        List<Transaction> deduplicated = items.stream()
                .collect(Collectors.toMap(
                        Transaction::getId,
                        Function.identity(),
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();

        return deduplicated.size() == items.size()
                ? this
                : new TransactionCollection(deduplicated);
    }

    public TransactionCollection topByAmount(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative");
        }
        if (limit == 0 || items.isEmpty()) {
            return EMPTY;
        }

        List<Transaction> top = items.stream()
                .sorted(Comparator
                        .comparing(Transaction::getAmount).reversed()
                        .thenComparing(Transaction::getCreatedAt)
                        .thenComparing(t -> t.getId().toString()))
                .limit(limit)
                .toList();

        return new TransactionCollection(top);
    }

    public Map<TransactionStatus, List<Transaction>> groupByStatus() {
        return items.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getStatus,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public Map<UUID, BigDecimal> sumAmountByAccount() {
        return items.stream()
                .collect(Collectors.toMap(
                        Transaction::getAccountId,
                        Transaction::getAmount,
                        BigDecimal::add,
                        LinkedHashMap::new
                ));
    }

    
    public boolean hasAnyRejected() {
        return items.stream()
                .anyMatch(t -> t.getStatus() == TransactionStatus.REJECTED);
    }
    
    public List<Transaction> toList() {
        return items;
    }

    
    public Transaction getById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return items.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No transaction found with id: " + id));
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionCollection other)) return false;
        return items.equals(other.items);
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }

    @Override
    public String toString() {
        return "TransactionCollection{size=" + items.size() + "}";
    }
}

