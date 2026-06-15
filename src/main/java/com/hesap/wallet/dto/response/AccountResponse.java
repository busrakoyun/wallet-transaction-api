package com.hesap.wallet.dto.response;

import com.hesap.wallet.entity.Account;
import com.hesap.wallet.enums.Currency;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * API view of an {@link Account}. Serialized as snake_case
 * ({@code account_id}, {@code user_id}, {@code created_at}, ...).
 */
public record AccountResponse(
        Long accountId,
        Long userId,
        Currency currency,
        BigDecimal balance,
        Instant createdAt
) {

    /** Maps a persisted entity to its API representation (entities never leave the service layer). */
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getCurrency(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }
}
