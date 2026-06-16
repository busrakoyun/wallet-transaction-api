package com.hesap.wallet.service;

import com.hesap.wallet.dto.request.CreateAccountRequest;
import com.hesap.wallet.dto.request.DepositRequest;
import com.hesap.wallet.dto.request.TransferRequest;
import com.hesap.wallet.dto.response.TransactionResponse;
import com.hesap.wallet.enums.Currency;
import com.hesap.wallet.enums.TransactionType;
import com.hesap.wallet.exception.AccountNotFoundException;
import com.hesap.wallet.repository.AccountRepository;
import com.hesap.wallet.repository.TransactionRepository;
import com.hesap.wallet.service.transfer.AbstractTransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end history checks against real H2: data is created through the actual deposit and
 * transfer flow, then read back to verify newest-first ordering and type filtering.
 */
@SpringBootTest
class TransactionHistoryIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private DepositService depositService;

    @Autowired
    private AbstractTransferService transferService;

    @Autowired
    private TransactionHistoryService historyService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void clean() {
        transactionRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }

    @Test
    void history_isNewestFirst_andFilterableByType() throws InterruptedException {
        Long a = accountService.createAccount(new CreateAccountRequest(1L, Currency.TRY)).accountId();
        Long b = accountService.createAccount(new CreateAccountRequest(2L, Currency.TRY)).accountId();

        // Small gaps guarantee strictly increasing timestamps for a deterministic order check.
        depositService.deposit(a, new DepositRequest(new BigDecimal("100.00")));
        Thread.sleep(10);
        depositService.deposit(a, new DepositRequest(new BigDecimal("50.00")));
        Thread.sleep(10);
        transferService.transfer(new TransferRequest(a, b, new BigDecimal("30.00")));

        List<TransactionResponse> history = historyService.getHistory(a, null);

        assertThat(history).hasSize(3);
        assertThat(history).extracting(TransactionResponse::createdAt)
                .isSortedAccordingTo(Comparator.reverseOrder()); // newest first
        assertThat(history.get(0).type()).isEqualTo(TransactionType.TRANSFER_OUT);

        TransactionResponse latest = history.get(0);
        assertThat(latest.amount()).isEqualByComparingTo("30.00");
        assertThat(latest.balanceAfter()).isEqualByComparingTo("120.00"); // 150 - 30
        assertThat(latest.relatedAccountId()).isEqualTo(b);

        // Filtering.
        assertThat(historyService.getHistory(a, TransactionType.DEPOSIT))
                .hasSize(2)
                .allSatisfy(tx -> assertThat(tx.type()).isEqualTo(TransactionType.DEPOSIT));
        assertThat(historyService.getHistory(a, TransactionType.TRANSFER_OUT)).hasSize(1);
        assertThat(historyService.getHistory(a, TransactionType.TRANSFER_IN)).isEmpty();

        // The receiver sees the matching TRANSFER_IN leg, linked back to the sender.
        assertThat(historyService.getHistory(b, null)).singleElement().satisfies(tx -> {
            assertThat(tx.type()).isEqualTo(TransactionType.TRANSFER_IN);
            assertThat(tx.balanceAfter()).isEqualByComparingTo("30.00");
            assertThat(tx.relatedAccountId()).isEqualTo(a);
        });
    }

    @Test
    void history_forUnknownAccount_throws() {
        assertThatThrownBy(() -> historyService.getHistory(999L, null))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
