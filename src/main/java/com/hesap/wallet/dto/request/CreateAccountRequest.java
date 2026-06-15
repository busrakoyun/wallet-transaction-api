package com.hesap.wallet.dto.request;

import com.hesap.wallet.enums.Currency;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating a wallet. Maps from snake_case JSON
 * ({@code user_id}, {@code currency}) via the global Jackson naming strategy.
 */
public record CreateAccountRequest(

        @NotNull(message = "user_id is required")
        Long userId,

        @NotNull(message = "currency is required")
        Currency currency
) {
}
