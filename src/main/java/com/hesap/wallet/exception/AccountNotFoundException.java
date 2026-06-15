package com.hesap.wallet.exception;

/**
 * Thrown when an operation references an account id that does not exist. Mapped to a
 * 404 / {@code ACCOUNT_NOT_FOUND} response by the global exception handler.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long accountId) {
        super("Account not found: " + accountId);
    }
}
