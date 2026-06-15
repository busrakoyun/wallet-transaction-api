package com.hesap.wallet.enums;

/**
 * Supported wallet currencies. Stored as STRING in the database so the schema
 * stays readable and is not coupled to enum ordinals.
 */
public enum Currency {
    TRY,
    USD,
    EUR
}
