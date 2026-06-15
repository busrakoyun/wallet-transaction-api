package com.hesap.wallet.service.transfer.validation;

import com.hesap.wallet.service.transfer.TransferContext;

/**
 * Strategy (GoF Behavioral) for a single transfer validation rule. The transfer engine
 * runs all registered strategies for the appropriate {@link ValidationPhase}; adding a
 * new rule means adding a new bean, with no change to the engine (Open/Closed Principle).
 */
public interface TransferValidationStrategy {

    /** Validates the transfer; throws a domain exception if the rule is violated. */
    void validate(TransferContext context);

    /** Whether this rule runs before or under the pessimistic lock. */
    ValidationPhase phase();
}
