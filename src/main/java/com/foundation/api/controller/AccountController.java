package com.foundation.api.controller;

import com.foundation.api.dto.AccountResponse;
import com.foundation.api.dto.OpenAccountRequest;
import com.foundation.domain.model.Account;
import com.foundation.repository.AccountRepository;
import com.foundation.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final TransactionService transactionService;

    public AccountController(AccountRepository accountRepository, TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest request) {
        Account account = transactionService.openAccount(request.customerId());
        AccountResponse body = AccountResponse.from(account);
        URI location = URI.create("/accounts/" + account.getId());
        return ResponseEntity.created(location).body(body);
    }


    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
