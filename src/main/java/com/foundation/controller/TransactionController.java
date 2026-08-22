package com.foundation.controller;

import com.foundation.domain.dto.AmountRequest;
import com.foundation.domain.dto.TransactionResponse;
import com.foundation.domain.dto.TransferRequest;
import com.foundation.domain.model.Transaction;
import com.foundation.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/accounts/{id}/deposits")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable UUID id,
            @Valid @RequestBody AmountRequest request) {

        Transaction transaction = transactionService.deposit(id, request.amount());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TransactionResponse.from(transaction));
    }

    @PostMapping("/accounts/{id}/withdrawals")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable UUID id,
            @Valid @RequestBody AmountRequest request) {

        Transaction transaction = transactionService.withdraw(id, request.amount());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TransactionResponse.from(transaction));
    }

    @PostMapping("/transfers")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {
        transactionService.transfer(
                request.sourceAccountId(),
                request.targetAccountId(),
                request.amount());
        return ResponseEntity.ok().build();
    }
}
