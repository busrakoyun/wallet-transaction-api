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
 * place. A transfer is recorded as a linked pair of {@code TRANSFER_OUT} / {@code TRANSFER_IN}
 * entries sharing a {@code transferReference}.
 *
 * <p>All builder methods snapshot {@code balanceAfter} from the supplied account, so the
 * account must already reflect the post-operation balance when the entry is built.
 */
@Component
public class TransactionFactory {

    /** Builds a successful {@code DEPOSIT} ledger entry. */
    public Transaction deposit(Account account, BigDecimal amount) {
        return Transaction.builder()
                .account(account)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .status(TransactionStatus.SUCCESS)
                .build();
    }

    /** Builds the sender-side {@code TRANSFER_OUT} leg, linked to the receiver. */
    public Transaction transferOut(Account sender, BigDecimal amount, Long receiverAccountId, String transferReference) {
        return Transaction.builder()
                .account(sender)
                .type(TransactionType.TRANSFER_OUT)
                .amount(amount)
                .balanceAfter(sender.getBalance())
                .status(TransactionStatus.SUCCESS)
                .relatedAccountId(receiverAccountId)
                .transferReference(transferReference)
                .build();
    }

    /** Builds the receiver-side {@code TRANSFER_IN} leg, linked to the sender. */
    public Transaction transferIn(Account receiver, BigDecimal amount, Long senderAccountId, String transferReference) {
        return Transaction.builder()
                .account(receiver)
                .type(TransactionType.TRANSFER_IN)
                .amount(amount)
                .balanceAfter(receiver.getBalance())
                .status(TransactionStatus.SUCCESS)
                .relatedAccountId(senderAccountId)
                .transferReference(transferReference)
                .build();
    }
}
