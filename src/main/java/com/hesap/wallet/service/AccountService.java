package com.hesap.wallet.service;

import com.hesap.wallet.dto.request.CreateAccountRequest;
import com.hesap.wallet.dto.response.AccountResponse;
import com.hesap.wallet.entity.Account;
import com.hesap.wallet.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Application service for wallet lifecycle operations. Returns DTOs so that JPA
 * entities never escape the service boundary.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    /** Scale (2 decimal places) applied to monetary amounts. */
    private static final int MONEY_SCALE = 2;

    private final AccountRepository accountRepository;

    /**
     * Creates a new wallet for the given user and currency with an initial balance of {@code 0.00}.
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = Account.builder()
                .userId(request.userId())
                .currency(request.currency())
                .balance(BigDecimal.ZERO.setScale(MONEY_SCALE))
                .build();

        Account saved = accountRepository.save(account);
        return AccountResponse.from(saved);
    }
}
