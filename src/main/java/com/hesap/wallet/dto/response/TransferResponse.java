package com.hesap.wallet.dto.response;

import com.hesap.wallet.entity.Transaction;
import com.hesap.wallet.enums.TransactionStatus;

import java.time.Instant;

/**
 * Result of a transfer. The {@code transaction_id} is the sender-side (TRANSFER_OUT)
 * ledger entry; both legs share a transfer reference internally. Serialized as snake_case
 * ({@code transaction_id}, {@code status}, {@code timestamp}).
 */
public record TransferResponse(
        Long transactionId,
        TransactionStatus status,
        Instant timestamp
) {

    public static TransferResponse from(Transaction transferOut) {
        return new TransferResponse(
                transferOut.getId(),
                transferOut.getStatus(),
                transferOut.getCreatedAt()
        );
    }
}
