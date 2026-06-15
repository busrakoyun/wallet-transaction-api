package com.hesap.wallet.dto.response;

import com.hesap.wallet.entity.Transaction;
import com.hesap.wallet.enums.TransactionStatus;

import java.math.BigDecimal;

/**
 * Result of a deposit. Serialized as snake_case
 * ({@code transaction_id}, {@code account_id}, {@code new_balance}, {@code status}).
 */
public record DepositResponse(
        Long transactionId,
        Long accountId,
        BigDecimal newBalance,
        TransactionStatus status
) {

    public static DepositResponse from(Transaction transaction) {
        return new DepositResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getBalanceAfter(),
                transaction.getStatus()
        );
    }
}
