package com.hesap.wallet.controller;

import com.hesap.wallet.dto.request.CreateAccountRequest;
import com.hesap.wallet.dto.request.DepositRequest;
import com.hesap.wallet.dto.response.AccountResponse;
import com.hesap.wallet.dto.response.DepositResponse;
import com.hesap.wallet.dto.response.TransactionResponse;
import com.hesap.wallet.enums.TransactionType;
import com.hesap.wallet.service.AccountService;
import com.hesap.wallet.service.DepositService;
import com.hesap.wallet.service.TransactionHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for wallet accounts. Delegates straight to the service layer
 * (Controller -> Service -> Repository; no facade).
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final DepositService depositService;
    private final TransactionHistoryService transactionHistoryService;

    /** Creates a wallet. Returns 201 Created with the new account representation. */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Tops up an account balance. Returns 200 OK with the resulting transaction and new balance. */
    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<DepositResponse> deposit(@PathVariable Long accountId,
                                                   @Valid @RequestBody DepositRequest request) {
        DepositResponse response = depositService.deposit(accountId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists an account's transactions newest-first, optionally filtered by
     * {@code ?transaction_type=DEPOSIT|TRANSFER_IN|TRANSFER_OUT}.
     */
    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable Long accountId,
            @RequestParam(name = "transaction_type", required = false) TransactionType transactionType) {
        List<TransactionResponse> history = transactionHistoryService.getHistory(accountId, transactionType);
        return ResponseEntity.ok(history);
    }
}
