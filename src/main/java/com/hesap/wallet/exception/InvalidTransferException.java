package com.hesap.wallet.exception;

/**
 * Thrown when a transfer request is structurally invalid (e.g. sender equals receiver,
 * non-positive amount, currency mismatch). Mapped to a 400 / {@code INVALID_TRANSFER}
 * response by the global exception handler.
 */
public class InvalidTransferException extends RuntimeException {

    public InvalidTransferException(String message) {
        super(message);
    }
}
