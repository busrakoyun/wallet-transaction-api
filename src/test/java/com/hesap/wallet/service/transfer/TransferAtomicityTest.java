package com.hesap.wallet.service.transfer;

import com.hesap.wallet.dto.request.TransferRequest;
import com.hesap.wallet.entity.Account;
import com.hesap.wallet.entity.Transaction;
import com.hesap.wallet.enums.Currency;
import com.hesap.wallet.enums.TransactionType;
import com.hesap.wallet.repository.AccountRepository;
import com.hesap.wallet.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

/**
 * Proves the transfer is atomic: a failure part-way through rolls back everything. The
 * first ledger leg (TRANSFER_OUT) is written for real, then the second (TRANSFER_IN) is
 * forced to fail inside the same transaction. Both balance changes and the first ledger
 * leg must be rolled back, leaving the system exactly as it started.
 */
@SpringBootTest
class TransferAtomicityTest {

    @Autowired
    private AbstractTransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @MockitoSpyBean
    private TransactionRepository transactionRepository;

    @BeforeEach
    void clean() {
        transactionRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }

    private Account seed(Currency currency, String balance) {
        return accountRepository.save(Account.builder()
                .userId(1L)
                .currency(currency)
                .balance(new BigDecimal(balance))
                .build());
    }

    @Test
    void failureOnSecondLedgerLeg_rollsBackBalancesAndFirstLeg() {
        Account sender = seed(Currency.TRY, "100.00");
        Account receiver = seed(Currency.TRY, "20.00");

        // The spy runs the real save for the OUT leg; only the IN leg is forced to fail
        // (doCallRealMethod is unavailable on an interface-backed repository proxy).
        doThrow(new RuntimeException("simulated ledger failure"))
                .when(transactionRepository).save(argThat((Transaction tx) ->
                        tx != null && tx.getType() == TransactionType.TRANSFER_IN));

        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(sender.getId(), receiver.getId(), new BigDecimal("30.00"))))
                .isInstanceOf(RuntimeException.class);

        // Balances are exactly as before the transfer.
        assertThat(accountRepository.findById(sender.getId()).orElseThrow().getBalance())
                .as("sender balance rolled back")
                .isEqualByComparingTo("100.00");
        assertThat(accountRepository.findById(receiver.getId()).orElseThrow().getBalance())
                .as("receiver balance rolled back")
                .isEqualByComparingTo("20.00");

        // Even the first ledger leg that "succeeded" was rolled back.
        assertThat(transactionRepository.count())
                .as("no ledger entries survive a failed transfer")
                .isZero();
    }
}
