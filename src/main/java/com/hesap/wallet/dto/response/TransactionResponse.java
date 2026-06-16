package com.hesap.wallet.dto.response;

import com.hesap.wallet.entity.Transaction;
import com.hesap.wallet.enums.TransactionStatus;
import com.hesap.wallet.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * API view of a ledger entry for transaction history. Serialized as snake_case
 * ({@code transaction_id}, {@code balance_after}, {@code related_account_id}, {@code created_at}).
 * {@code related_account_id} is the transfer counterparty and is omitted for deposits.
 *
 * <p>Maps only entity columns (never the lazy {@code account} association), so it is safe
 * to build outside an open persistence session.
 */
public record TransactionResponse(
        Long transactionId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        TransactionStatus status,
        Long relatedAccountId,
        Instant createdAt
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getStatus(),
                transaction.getRelatedAccountId(),
                transaction.getCreatedAt()
        );
    }
}
