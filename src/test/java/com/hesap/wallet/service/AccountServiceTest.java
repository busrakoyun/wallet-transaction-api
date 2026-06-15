package com.hesap.wallet.service;

import com.hesap.wallet.dto.request.CreateAccountRequest;
import com.hesap.wallet.dto.response.AccountResponse;
import com.hesap.wallet.entity.Account;
import com.hesap.wallet.enums.Currency;
import com.hesap.wallet.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccount_startsWithZeroBalanceAndPersistsRequestedFields() {
        CreateAccountRequest request = new CreateAccountRequest(42L, Currency.TRY);
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        // Simulate JPA assigning the id and creation timestamp on save.
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account toSave = invocation.getArgument(0);
            return Account.builder()
                    .id(1L)
                    .userId(toSave.getUserId())
                    .currency(toSave.getCurrency())
                    .balance(toSave.getBalance())
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build();
        });

        AccountResponse response = accountService.createAccount(request);

        assertThat(response.accountId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(42L);
        assertThat(response.currency()).isEqualTo(Currency.TRY);
        assertThat(response.balance()).isEqualByComparingTo("0.00");
        assertThat(response.createdAt()).isEqualTo(createdAt);

        // The entity handed to the repository must start at exactly zero.
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account persisted = captor.getValue();
        assertThat(persisted.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(persisted.getUserId()).isEqualTo(42L);
        assertThat(persisted.getCurrency()).isEqualTo(Currency.TRY);
    }
}
