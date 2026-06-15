package com.hesap.wallet.service;

import com.hesap.wallet.dto.request.CreateAccountRequest;
import com.hesap.wallet.dto.request.DepositRequest;
import com.hesap.wallet.dto.response.AccountResponse;
import com.hesap.wallet.entity.Account;
import com.hesap.wallet.enums.Currency;
import com.hesap.wallet.repository.AccountRepository;
import com.hesap.wallet.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that the {@code PESSIMISTIC_WRITE} lock on {@link AccountRepository#findByIdForUpdate}
 * serializes concurrent balance mutations. Many threads deposit into one account
 * simultaneously; with the lock the final balance equals the exact sum of all deposits.
 * Without the lock the interleaved read-modify-write cycles would lose updates and the
 * balance would fall short.
 */
@SpringBootTest
class DepositConcurrencyTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private DepositService depositService;

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
    void concurrentDeposits_areSerialized_noLostUpdates() throws InterruptedException {
        AccountResponse account = accountService.createAccount(new CreateAccountRequest(1L, Currency.TRY));
        Long accountId = account.accountId();

        int threads = 16;
        int depositsPerThread = 25;
        BigDecimal amount = new BigDecimal("1.00");
        int totalDeposits = threads * depositsPerThread;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(threads);
        AtomicInteger failures = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    startGate.await(); // line everyone up so they hit the row together
                    for (int i = 0; i < depositsPerThread; i++) {
                        depositService.deposit(accountId, new DepositRequest(amount));
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    finished.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = finished.await(30, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(completed).as("all deposit threads finished in time").isTrue();
        assertThat(failures.get()).as("no deposit threw").isZero();

        BigDecimal expected = amount.multiply(BigDecimal.valueOf(totalDeposits)); // 400.00
        Account reloaded = accountRepository.findById(accountId).orElseThrow();
        assertThat(reloaded.getBalance())
                .as("final balance equals the exact sum of all concurrent deposits")
                .isEqualByComparingTo(expected);

        assertThat(transactionRepository.count())
                .as("exactly one DEPOSIT ledger entry per deposit")
                .isEqualTo(totalDeposits);
    }
}
