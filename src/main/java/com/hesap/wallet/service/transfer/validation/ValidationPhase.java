package com.hesap.wallet.service.transfer.validation;

/**
 * When a {@link TransferValidationStrategy} runs within the transfer template.
 */
public enum ValidationPhase {

    /** Stateless request checks, run before any lock is acquired (template step 1). */
    PRE_LOCK,

    /**
     * Balance/state checks that must read the lock-protected accounts, run under the
     * pessimistic lock immediately before the balances are mutated (template step 3).
     */
    POST_LOCK
}
