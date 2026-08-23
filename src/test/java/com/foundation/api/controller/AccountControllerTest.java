package com.foundation.api.controller;

import com.foundation.domain.model.Account;
import com.foundation.repository.AccountRepository;
import com.foundation.service.TransactionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private AccountRepository accountRepository;

    @Nested
    class OpenAccount {

        @Test
        void openAccount_shouldReturn201WithBodyAndLocation() throws Exception {
            Account account = new Account("customer-1");
            when(transactionService.openAccount("customer-1")).thenReturn(account);

            mockMvc.perform(post("/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"customerId\": \"customer-1\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/accounts/" + account.getId()))
                    .andExpect(jsonPath("$.id").value(account.getId().toString()))
                    .andExpect(jsonPath("$.customerId").value("customer-1"))
                    .andExpect(jsonPath("$.balance").value(0));

            verify(transactionService).openAccount("customer-1");
        }

        @Test
        void openAccount_shouldReturn400WhenCustomerIdIsBlank() throws Exception {
            mockMvc.perform(post("/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"customerId\": \"\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(transactionService);
        }

        @Test
        void openAccount_shouldReturn400WhenBodyIsMalformed() throws Exception {
            mockMvc.perform(post("/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not valid json "))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class GetAccount {

        @Test
        void getAccount_shouldReturn200WhenAccountExists() throws Exception {
            Account account = new Account("customer-1");
            when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

            mockMvc.perform(get("/accounts/{id}", account.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(account.getId().toString()))
                    .andExpect(jsonPath("$.customerId").value("customer-1"))
                    .andExpect(jsonPath("$.balance").value(0));
        }

        @Test
        void getAccount_shouldReturn404WhenAccountDoesNotExist() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(accountRepository.findById(unknownId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/accounts/{id}", unknownId))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getAccount_shouldReturn400WhenIdIsNotAUuid() throws Exception {
            mockMvc.perform(get("/accounts/{id}", "not-a-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }
}
