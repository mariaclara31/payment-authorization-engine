package com.foundation.domain.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionCollectionTest {

    
    
    

    @Nested
    class Factory {

        @Test
        void of_shouldRejectNullInput() {
            assertThrows(NullPointerException.class, () -> TransactionCollection.of(null));
        }

        @Test
        void of_shouldReturnSharedEmptyInstanceForEmptyList() {
            assertSame(TransactionCollection.empty(), TransactionCollection.of(List.of()));
        }

        @Test
        void empty_shouldBeAnEmptyCollection() {
            assertTrue(TransactionCollection.empty().isEmpty());
            assertEquals(0, TransactionCollection.empty().size());
        }
    }

    
    
    

    @Nested
    class Filtering {

        @Test
        void withStatus_shouldReturnOnlyMatchingTransactions() {
            Transaction approved = transaction("customer-1", "100.00", TransactionStatus.APPROVED);
            Transaction rejected = transaction("customer-2", "50.00", TransactionStatus.REJECTED);
            Transaction pending  = transaction("customer-3", "75.00", TransactionStatus.PENDING);

            TransactionCollection result = TransactionCollection
                    .of(List.of(approved, rejected, pending))
                    .withStatus(TransactionStatus.APPROVED);

            assertEquals(List.of(approved), result.toList());
        }

        @Test
        void approved_shouldBeConvenienceForApprovedStatus() {
            Transaction approved = transaction("customer-1", "100.00", TransactionStatus.APPROVED);
            Transaction rejected = transaction("customer-2", "50.00", TransactionStatus.REJECTED);

            assertEquals(
                    List.of(approved),
                    TransactionCollection.of(List.of(approved, rejected)).approved().toList()
            );
        }

        @Test
        void rejected_shouldBeConvenienceForRejectedStatus() {
            Transaction approved = transaction("customer-1", "100.00", TransactionStatus.APPROVED);
            Transaction rejected = transaction("customer-2", "50.00", TransactionStatus.REJECTED);

            assertEquals(
                    List.of(rejected),
                    TransactionCollection.of(List.of(approved, rejected)).rejected().toList()
            );
        }

        @Test
        void filter_shouldReturnSharedEmptyInstanceWhenNoMatchFound() {
            Transaction approved = transaction("customer-1", "100.00", TransactionStatus.APPROVED);

            assertSame(
                    TransactionCollection.empty(),
                    TransactionCollection.of(List.of(approved)).withStatus(TransactionStatus.REJECTED)
            );
        }

        @Test
        void filter_shouldRejectNullPredicate() {
            assertThrows(NullPointerException.class,
                    () -> TransactionCollection.empty().filter(null));
        }

        @Test
        void filter_shouldBeComposable() {
            Transaction highApproved = transaction("customer-1", "500.00", TransactionStatus.APPROVED);
            Transaction lowApproved  = transaction("customer-2", "10.00",  TransactionStatus.APPROVED);
            Transaction rejected     = transaction("customer-1", "300.00", TransactionStatus.REJECTED);

            List<Transaction> result = TransactionCollection
                    .of(List.of(highApproved, lowApproved, rejected))
                    .approved()
                    .filter(t -> t.getAmount().compareTo(new BigDecimal("100.00")) > 0)
                    .toList();

            assertEquals(List.of(highApproved), result);
        }
    }

    
    
    

    @Nested
    class Deduplication {

        @Test
        void deduplicateById_shouldKeepFirstOccurrenceAndPreserveOrder() {
            UUID sharedId = UUID.randomUUID();
            Transaction first           = transaction(sharedId, "customer-1", "100.00", TransactionStatus.APPROVED);
            Transaction second          = transaction(UUID.randomUUID(), "customer-2", "50.00", TransactionStatus.PENDING);
            Transaction duplicateOfFirst = transaction(sharedId, "customer-1", "100.00", TransactionStatus.APPROVED);

            TransactionCollection result = TransactionCollection
                    .of(List.of(first, second, duplicateOfFirst))
                    .deduplicateById();

            assertEquals(List.of(first, second), result.toList());
        }

        @Test
        void deduplicateById_shouldReturnSameInstanceWhenNoDuplicatesExist() {
            TransactionCollection original = TransactionCollection.of(List.of(
                    transaction("customer-1", "100.00", TransactionStatus.APPROVED)
            ));

            assertSame(original, original.deduplicateById());
        }

        @Test
        void deduplicateById_shouldReturnNewInstanceWhenDuplicatesAreRemoved() {
            UUID sharedId = UUID.randomUUID();
            Transaction tx  = transaction(sharedId, "customer-1", "100.00", TransactionStatus.APPROVED);
            Transaction dup = transaction(sharedId, "customer-1", "100.00", TransactionStatus.APPROVED);

            TransactionCollection original = TransactionCollection.of(List.of(tx, dup));
            TransactionCollection result   = original.deduplicateById();

            assertNotSame(original, result);
            assertEquals(1, result.size());
        }
    }

    
    
    

    @Nested
    class TopByAmount {

        @Test
        void topByAmount_shouldReturnHighestAmountsRespectingLimit() {
            Transaction low    = transaction("customer-1", "10.00",  TransactionStatus.APPROVED);
            Transaction high   = transaction("customer-2", "300.00", TransactionStatus.APPROVED);
            Transaction middle = transaction("customer-3", "120.00", TransactionStatus.PENDING);

            List<Transaction> result = TransactionCollection
                    .of(List.of(low, high, middle))
                    .topByAmount(2)
                    .toList();

            assertEquals(List.of(high, middle), result);
        }

        @Test
        void topByAmount_shouldReturnSharedEmptyInstanceWhenLimitIsZero() {
            Transaction tx = transaction("customer-1", "100.00", TransactionStatus.APPROVED);

            assertSame(TransactionCollection.empty(),
                    TransactionCollection.of(List.of(tx)).topByAmount(0));
        }

        @Test
        void topByAmount_shouldRejectNegativeLimit() {
            assertThrows(IllegalArgumentException.class,
                    () -> TransactionCollection.empty().topByAmount(-1));
        }

        @Test
        void topByAmount_shouldBreakTiesDeterministicallyByCreatedAt() {
            LocalDateTime earlier = LocalDateTime.of(2026, 1, 1, 10, 0);
            LocalDateTime later   = LocalDateTime.of(2026, 1, 1, 11, 0);

            Transaction tieA = transaction(UUID.randomUUID(), "customer-1", "100.00", TransactionStatus.APPROVED, earlier);
            Transaction tieB = transaction(UUID.randomUUID(), "customer-2", "100.00", TransactionStatus.APPROVED, later);

            List<Transaction> result = TransactionCollection
                    .of(List.of(tieB, tieA))
                    .topByAmount(2)
                    .toList();

            assertEquals(List.of(tieA, tieB), result);
        }
    }

    
    
    

    @Nested
    class Aggregations {

        @Test
        void groupByStatus_shouldGroupTransactionsByStatus() {
            Transaction approved        = transaction("customer-1", "100.00", TransactionStatus.APPROVED);
            Transaction rejected        = transaction("customer-2", "50.00",  TransactionStatus.REJECTED);
            Transaction anotherApproved = transaction("customer-3", "75.00",  TransactionStatus.APPROVED);

            Map<TransactionStatus, List<Transaction>> result = TransactionCollection
                    .of(List.of(approved, rejected, anotherApproved))
                    .groupByStatus();

            assertEquals(List.of(approved, anotherApproved), result.get(TransactionStatus.APPROVED));
            assertEquals(List.of(rejected),                  result.get(TransactionStatus.REJECTED));
        }

        @Test
        void sumAmountByCustomer_shouldSumAllTransactionsInThisCollection() {
            List<Transaction> transactions = List.of(
                    transaction("customer-1", "100.10", TransactionStatus.APPROVED),
                    transaction("customer-1", "30.90",  TransactionStatus.APPROVED),
                    transaction("customer-2", "20.00",  TransactionStatus.APPROVED)
            );

            Map<String, BigDecimal> result = TransactionCollection.of(transactions)
                    .sumAmountByCustomer();

            assertEquals(new BigDecimal("131.00"), result.get("customer-1"));
            assertEquals(new BigDecimal("20.00"),  result.get("customer-2"));
        }

        @Test
        void sumAmountByCustomer_composedWithApproved_shouldIgnoreNonApprovedTransactions() {
            List<Transaction> transactions = List.of(
                    transaction("customer-1", "100.00", TransactionStatus.APPROVED),
                    transaction("customer-1", "999.00", TransactionStatus.REJECTED),
                    transaction("customer-2", "20.00",  TransactionStatus.APPROVED),
                    transaction("customer-2", "888.00", TransactionStatus.PENDING)
            );

            Map<String, BigDecimal> result = TransactionCollection.of(transactions)
                    .approved()
                    .sumAmountByCustomer();

            assertEquals(new BigDecimal("100.00"), result.get("customer-1"));
            assertEquals(new BigDecimal("20.00"),  result.get("customer-2"));
        }

        @Test
        void hasAnyRejected_shouldReturnTrueOnlyWhenRejectedExists() {
            TransactionCollection withRejected = TransactionCollection.of(List.of(
                    transaction("customer-1", "10.00", TransactionStatus.APPROVED),
                    transaction("customer-2", "20.00", TransactionStatus.REJECTED)
            ));
            TransactionCollection withoutRejected = TransactionCollection.of(List.of(
                    transaction("customer-3", "30.00", TransactionStatus.APPROVED)
            ));

            assertTrue(withRejected.hasAnyRejected());
            assertFalse(withoutRejected.hasAnyRejected());
        }
    }

    
    
    

    @Nested
    class EmptyCollectionEdgeCases {

        @Test
        void allOperations_shouldHandleEmptyCollectionGracefully() {
            TransactionCollection empty = TransactionCollection.empty();

            assertEquals(List.of(), empty.toList());
            assertEquals(Map.of(),  empty.groupByStatus());
            assertEquals(Map.of(),  empty.sumAmountByCustomer());
            assertEquals(List.of(), empty.topByAmount(5).toList());
            assertFalse(empty.hasAnyRejected());
            assertSame(empty, empty.deduplicateById());
        }
    }

    
    
    

    private static Transaction transaction(String customerId, String amount, TransactionStatus status) {
        return transaction(UUID.randomUUID(), customerId, amount, status, LocalDateTime.of(2026, 5, 14, 12, 0));
    }

    private static Transaction transaction(UUID id, String customerId, String amount, TransactionStatus status) {
        return transaction(id, customerId, amount, status, LocalDateTime.of(2026, 5, 14, 12, 0));
    }

    private static Transaction transaction(UUID id, String customerId, String amount,
                                           TransactionStatus status, LocalDateTime createdAt) {
        return new Transaction(id, customerId, new BigDecimal(amount), TransactionType.PAYMENT, status, createdAt);
    }
}

