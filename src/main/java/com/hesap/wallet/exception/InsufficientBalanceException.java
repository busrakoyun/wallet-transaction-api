package com.hesap.wallet.exception;

/**
 * Thrown when a sender lacks the funds to cover a transfer. Evaluated under the
 * pessimistic lock so the check and the debit are race-free. Mapped to a
 * 422 / {@code INSUFFICIENT_BALANCE} response by the global exception handler.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(Long accountId) {
        super("Insufficient balance in account: " + accountId);
    }
}
