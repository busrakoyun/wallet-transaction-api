package com.hesap.wallet.enums;

/**
 * Ledger entry types. A deposit produces a single {@link #DEPOSIT} entry; a transfer
 * produces a linked pair of {@link #TRANSFER_OUT} (sender) and {@link #TRANSFER_IN}
 * (receiver) entries (double-entry bookkeeping).
 */
public enum TransactionType {
    DEPOSIT,
    TRANSFER_IN,
    TRANSFER_OUT
}
