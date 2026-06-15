package com.hesap.wallet.service.transfer;

import com.hesap.wallet.entity.Account;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Mutable carrier passed through the transfer template steps. Created from the request
 * with immutable inputs; the locked account instances and the transfer reference are
 * populated by later steps. Keeps step signatures small and the engine readable.
 */
@Getter
@Setter
public class TransferContext {

    private final Long senderAccountId;
    private final Long receiverAccountId;
    private final BigDecimal amount;

    /** Populated during the lock-acquisition step. */
    private Account sender;
    private Account receiver;

    /** Shared reference linking the two ledger legs; set during the ledger step. */
    private String transferReference;

    public TransferContext(Long senderAccountId, Long receiverAccountId, BigDecimal amount) {
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.amount = amount;
    }
}
