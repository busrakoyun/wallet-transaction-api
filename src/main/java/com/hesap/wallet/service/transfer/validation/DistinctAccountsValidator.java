package com.hesap.wallet.service.transfer.validation;

import com.hesap.wallet.exception.InvalidTransferException;
import com.hesap.wallet.service.transfer.TransferContext;
import org.springframework.stereotype.Component;

/**
 * Rejects transfers where the sender and receiver are the same account. Stateless, so it
 * runs before locking (and guarantees the lock step never tries to lock one account twice).
 */
@Component
public class DistinctAccountsValidator implements TransferValidationStrategy {

    @Override
    public void validate(TransferContext context) {
        if (context.getSenderAccountId().equals(context.getReceiverAccountId())) {
            throw new InvalidTransferException("sender and receiver accounts must be different");
        }
    }

    @Override
    public ValidationPhase phase() {
        return ValidationPhase.PRE_LOCK;
    }
}
