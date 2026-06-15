package com.hesap.wallet.enums;

/**
 * Outcome of a ledger entry. Successful operations are committed atomically; failed
 * operations are rolled back, so persisted entries are normally {@link #SUCCESS}.
 */
public enum TransactionStatus {
    SUCCESS,
    FAILED
}
