package com.hesap.wallet.service.transfer.validation;

import com.hesap.wallet.exception.InvalidTransferException;
import com.hesap.wallet.service.transfer.TransferContext;
import org.springframework.stereotype.Component;

/**
 * Rejects transfers between accounts of different currencies. Reads the loaded accounts,
 * so it runs under the lock.
 */
@Component
public class SameCurrencyValidator implements TransferValidationStrategy {

    @Override
    public void validate(TransferContext context) {
        if (context.getSender().getCurrency() != context.getReceiver().getCurrency()) {
            throw new InvalidTransferException(
                    "currency mismatch: sender is " + context.getSender().getCurrency()
                            + ", receiver is " + context.getReceiver().getCurrency());
        }
    }

    @Override
    public ValidationPhase phase() {
        return ValidationPhase.POST_LOCK;
    }
}
