package com.hesap.wallet.factory;

import com.hesap.wallet.entity.Account;
import com.hesap.wallet.entity.Transaction;
import com.hesap.wallet.enums.TransactionStatus;
import com.hesap.wallet.enums.TransactionType;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

/**
 * Factory (GoF Creational) that centralizes construction of {@link Transaction} ledger
 * entries, keeping persistence rules (type, status, balance snapshot, linkage) in one
 * place. Transfer entries (TRANSFER_OUT / TRANSFER_IN) are added with the transfer engine.
 */
@Component
public class TransactionFactory {

    /**
     * Builds a successful {@code DEPOSIT} ledger entry. The {@code account} must already
     * reflect the post-deposit balance, which is snapshotted into {@code balanceAfter}.
     */
    public Transaction deposit(Account account, BigDecimal amount) {
        return Transaction.builder()
                .account(account)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .status(TransactionStatus.SUCCESS)
                .build();
    }
}
