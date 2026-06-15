package com.hesap.wallet.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request body for a top-up. The amount must be positive and fit the monetary scale
 * (up to 2 decimal places) to match the {@code numeric(19,2)} balance column.
 */
public record DepositRequest(

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "amount must have at most 2 decimal places")
        BigDecimal amount
) {
}
