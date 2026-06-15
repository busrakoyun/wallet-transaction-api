package com.hesap.wallet.service.transfer.validation;

import com.hesap.wallet.exception.InvalidTransferException;
import com.hesap.wallet.service.transfer.TransferContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Rejects non-positive transfer amounts. Stateless, so it runs before locking.
 */
@Component
public class PositiveAmountValidator implements TransferValidationStrategy {

    @Override
    public void validate(TransferContext context) {
        BigDecimal amount = context.getAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransferException("amount must be greater than zero");
        }
    }

    @Override
    public ValidationPhase phase() {
        return ValidationPhase.PRE_LOCK;
    }
}
