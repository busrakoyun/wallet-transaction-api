package com.hesap.wallet.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request body for an internal transfer. Binds from snake_case JSON
 * ({@code sender_account_id}, {@code receiver_account_id}, {@code amount}).
 */
public record TransferRequest(

        @NotNull(message = "sender_account_id is required")
        Long senderAccountId,

        @NotNull(message = "receiver_account_id is required")
        Long receiverAccountId,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "amount must have at most 2 decimal places")
        BigDecimal amount
) {
}
