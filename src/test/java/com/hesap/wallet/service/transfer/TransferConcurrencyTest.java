package com.hesap.wallet.service.transfer;

import com.hesap.wallet.dto.request.TransferRequest;
import com.hesap.wallet.entity.Account;
import com.hesap.wallet.enums.Currency;
import com.hesap.wallet.exception.InsufficientBalanceException;
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
 * Concurrency proofs for the transfer engine on real H2.
 */
@SpringBootTest
class TransferConcurrencyTest {

    @Autowired
    private AbstractTransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
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

    private BigDecimal balanceOf(Long id) {
        return accountRepository.findById(id).orElseThrow().getBalance();
    }

    @Test
    void concurrentTransfers_neverOverdraw_andConserveMoney() throws InterruptedException {
        Account sender = seed(Currency.TRY, "100.00");
        Account receiver = seed(Currency.TRY, "0.00");

        int threads = 50;
        BigDecimal amount = new BigDecimal("10.00"); // only 10 of 50 attempts can be funded
        int fundableTransfers = 10;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    transferService.transfer(new TransferRequest(sender.getId(), receiver.getId(), amount));
                    succeeded.incrementAndGet();
                } catch (InsufficientBalanceException e) {
                    insufficient.incrementAndGet();
                } catch (Exception e) {
                    unexpected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = done.await(30, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(completed).as("all transfer attempts finished").isTrue();
        assertThat(unexpected.get()).as("no unexpected errors").isZero();
        assertThat(succeeded.get()).as("exactly the fundable number of transfers succeed").isEqualTo(fundableTransfers);
        assertThat(insufficient.get()).as("the rest are rejected for insufficient funds").isEqualTo(threads - fundableTransfers);

        BigDecimal senderFinal = balanceOf(sender.getId());
        BigDecimal receiverFinal = balanceOf(receiver.getId());
        assertThat(senderFinal).as("sender drained but never negative").isEqualByComparingTo("0.00");
        assertThat(receiverFinal).isEqualByComparingTo("100.00");
        assertThat(senderFinal.add(receiverFinal)).as("total money conserved").isEqualByComparingTo("100.00");

        assertThat(transactionRepository.count())
                .as("two ledger legs per successful transfer")
                .isEqualTo(fundableTransfers * 2L);
    }

    @Test
    void bidirectionalTransfers_doNotDeadlock_andConserveMoney() throws InterruptedException {
        Account a = seed(Currency.TRY, "1000.00");
        Account b = seed(Currency.TRY, "1000.00");

        int perDirection = 50;
        BigDecimal amount = new BigDecimal("1.00");

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(perDirection * 2);
        AtomicInteger errors = new AtomicInteger();

        Runnable aToB = () -> {
            try {
                startGate.await();
                transferService.transfer(new TransferRequest(a.getId(), b.getId(), amount));
            } catch (Exception e) {
                errors.incrementAndGet();
            } finally {
                done.countDown();
            }
        };
        Runnable bToA = () -> {
            try {
                startGate.await();
                transferService.transfer(new TransferRequest(b.getId(), a.getId(), amount));
            } catch (Exception e) {
                errors.incrementAndGet();
            } finally {
                done.countDown();
            }
        };

        for (int i = 0; i < perDirection; i++) {
            pool.submit(aToB);
            pool.submit(bToA);
        }

        startGate.countDown();
        // If the lock order were inconsistent, opposing transfers would deadlock and this would time out.
        boolean completed = done.await(30, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(completed).as("no deadlock: every transfer completed").isTrue();
        assertThat(errors.get()).as("no errors under bidirectional contention").isZero();

        BigDecimal aFinal = balanceOf(a.getId());
        BigDecimal bFinal = balanceOf(b.getId());
        // Equal transfers in both directions net to zero regardless of interleaving.
        assertThat(aFinal).isEqualByComparingTo("1000.00");
        assertThat(bFinal).isEqualByComparingTo("1000.00");
        assertThat(aFinal.add(bFinal)).as("total money conserved").isEqualByComparingTo("2000.00");
        assertThat(transactionRepository.count()).isEqualTo(perDirection * 2L * 2L);
    }
}
