package com.hesap.wallet.factory;

import com.hesap.wallet.entity.Account;
import com.hesap.wallet.entity.Transaction;
import com.hesap.wallet.enums.Currency;
import com.hesap.wallet.enums.TransactionStatus;
import com.hesap.wallet.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionFactoryTest {

    private final TransactionFactory factory = new TransactionFactory();

    @Test
    void deposit_buildsSuccessfulDepositEntrySnapshottingBalance() {
        Account account = Account.builder()
                .id(1L)
                .userId(7L)
                .currency(Currency.USD)
                .balance(new BigDecimal("150.00")) // already reflects the deposit
                .build();

        Transaction tx = factory.deposit(account, new BigDecimal("50.00"));

        assertThat(tx.getAccount()).isSameAs(account);
        assertThat(tx.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(tx.getAmount()).isEqualByComparingTo("50.00");
        assertThat(tx.getBalanceAfter()).isEqualByComparingTo("150.00");
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(tx.getRelatedAccountId()).isNull();
        assertThat(tx.getTransferReference()).isNull();
    }
}
