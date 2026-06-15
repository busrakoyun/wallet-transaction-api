package com.hesap.wallet.service.transfer.validation;

import com.hesap.wallet.exception.InsufficientBalanceException;
import com.hesap.wallet.service.transfer.TransferContext;
import org.springframework.stereotype.Component;

/**
 * Rejects transfers when the sender cannot cover the amount. Reads the lock-protected
 * sender balance, so it runs under the lock immediately before the debit — making the
 * funds check and the debit a single race-free unit.
 */
@Component
public class SufficientBalanceValidator implements TransferValidationStrategy {

    @Override
    public void validate(TransferContext context) {
        if (context.getSender().getBalance().compareTo(context.getAmount()) < 0) {
            throw new InsufficientBalanceException(context.getSenderAccountId());
        }
    }

    @Override
    public ValidationPhase phase() {
        return ValidationPhase.POST_LOCK;
    }
}
