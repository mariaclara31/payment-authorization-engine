package com.paymentengine.api.controller;

import com.paymentengine.domain.exception.AccountNotFoundException;
import com.paymentengine.domain.exception.InsufficientBalanceException;
import com.paymentengine.domain.model.Transaction;
import com.paymentengine.domain.model.TransactionStatus;
import com.paymentengine.domain.model.TransactionType;
import com.paymentengine.service.TransactionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureRestTestClient
class TransactionControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private TransactionService transactionService;

    private static Transaction sampleTransaction(UUID accountId, String amount,
                                                 TransactionType type) {
        return new Transaction(
                UUID.randomUUID(),
                accountId,
                new BigDecimal(amount),
                type,
                TransactionStatus.APPROVED,
                LocalDateTime.of(2026, 5, 14, 12, 0)
        );
    }

    @Nested
    class Deposit {

        @Test
        void deposit_shouldReturn201WithTransaction() {
            UUID accountId = UUID.randomUUID();
            Transaction tx = sampleTransaction(accountId, "100.00", TransactionType.DEPOSIT);
            when(transactionService.deposit(eq(accountId), any(BigDecimal.class))).thenReturn(tx);

            restTestClient.post()
                    .uri("/accounts/{id}/deposits", accountId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"amount\": 100.00}")
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.accountId").isEqualTo(accountId.toString())
                    .jsonPath("$.type").isEqualTo("DEPOSIT")
                    .jsonPath("$.status").isEqualTo("APPROVED")
                    .jsonPath("$.amount").isEqualTo(100.00);
        }

        @Test
        void deposit_shouldReturn404WhenAccountDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(transactionService.deposit(eq(unknownId), any(BigDecimal.class)))
                    .thenThrow(AccountNotFoundException.forId(unknownId));

            restTestClient.post()
                    .uri("/accounts/{id}/deposits", unknownId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"amount\": 50.00}")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(404)
                    .jsonPath("$.message").exists();
        }

        @Test
        void deposit_shouldReturn400WhenAmountIsNegative() {
            UUID accountId = UUID.randomUUID();

            restTestClient.post()
                    .uri("/accounts/{id}/deposits", accountId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"amount\": -5.00}")
                    .exchange()
                    .expectStatus().isBadRequest();

            verifyNoInteractions(transactionService);
        }

        @Test
        void deposit_shouldReturn400WhenAmountIsMissing() {
            UUID accountId = UUID.randomUUID();

            restTestClient.post()
                    .uri("/accounts/{id}/deposits", accountId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    class Withdraw {

        @Test
        void withdraw_shouldReturn201WithTransaction() {
            UUID accountId = UUID.randomUUID();
            Transaction tx = sampleTransaction(accountId, "30.00", TransactionType.WITHDRAWAL);
            when(transactionService.withdraw(eq(accountId), any(BigDecimal.class))).thenReturn(tx);

            restTestClient.post()
                    .uri("/accounts/{id}/withdrawals", accountId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"amount\": 30.00}")
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.type").isEqualTo("WITHDRAWAL");
        }

        @Test
        void withdraw_shouldReturn422WhenBalanceInsufficient() {
            UUID accountId = UUID.randomUUID();
            when(transactionService.withdraw(eq(accountId), any(BigDecimal.class)))
                    .thenThrow(new InsufficientBalanceException("insufficient balance"));

            restTestClient.post()
                    .uri("/accounts/{id}/withdrawals", accountId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"amount\": 999999.00}")
                    .exchange()
                    .expectStatus().isEqualTo(422)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(422)
                    .jsonPath("$.message").isEqualTo("insufficient balance");
        }
    }

    @Nested
    class Transfer {

        @Test
        void transfer_shouldReturn200OnSuccess() {
            UUID source = UUID.randomUUID();
            UUID target = UUID.randomUUID();
            doNothing().when(transactionService)
                    .transfer(eq(source), eq(target), any(BigDecimal.class));

            restTestClient.post()
                    .uri("/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"sourceAccountId\": \"" + source + "\", " +
                            "\"targetAccountId\": \"" + target + "\", " +
                            "\"amount\": 20.00}")
                    .exchange()
                    .expectStatus().isOk();

            verify(transactionService).transfer(eq(source), eq(target), any(BigDecimal.class));
        }

        @Test
        void transfer_shouldReturn400WhenTransferringToSameAccount() {
            UUID accountId = UUID.randomUUID();
            doThrow(new IllegalArgumentException("Cannot transfer to the same account"))
                    .when(transactionService)
                    .transfer(eq(accountId), eq(accountId), any(BigDecimal.class));

            restTestClient.post()
                    .uri("/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"sourceAccountId\": \"" + accountId + "\", " +
                            "\"targetAccountId\": \"" + accountId + "\", " +
                            "\"amount\": 10.00}")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Cannot transfer to the same account");
        }

        @Test
        void transfer_shouldReturn404WhenAnAccountDoesNotExist() {
            UUID source = UUID.randomUUID();
            UUID target = UUID.randomUUID();
            doThrow(AccountNotFoundException.forId(source))
                    .when(transactionService)
                    .transfer(eq(source), eq(target), any(BigDecimal.class));

            restTestClient.post()
                    .uri("/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"sourceAccountId\": \"" + source + "\", " +
                            "\"targetAccountId\": \"" + target + "\", " +
                            "\"amount\": 10.00}")
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        void transfer_shouldReturn400WhenAmountIsNegative() {
            UUID source = UUID.randomUUID();
            UUID target = UUID.randomUUID();

            restTestClient.post()
                    .uri("/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"sourceAccountId\": \"" + source + "\", " +
                            "\"targetAccountId\": \"" + target + "\", " +
                            "\"amount\": -10.00}")
                    .exchange()
                    .expectStatus().isBadRequest();

            verifyNoInteractions(transactionService);
        }
    }
}