package com.hesap.wallet.controller;

import com.hesap.wallet.dto.request.CreateAccountRequest;
import com.hesap.wallet.dto.request.DepositRequest;
import com.hesap.wallet.dto.response.AccountResponse;
import com.hesap.wallet.dto.response.DepositResponse;
import com.hesap.wallet.service.AccountService;
import com.hesap.wallet.service.DepositService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
