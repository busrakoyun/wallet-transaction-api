package com.hesap.wallet.service.transfer;

import com.hesap.wallet.dto.request.TransferRequest;
import com.hesap.wallet.dto.response.TransferResponse;
import com.hesap.wallet.entity.Account;
import com.hesap.wallet.entity.Transaction;
import com.hesap.wallet.enums.Currency;
import com.hesap.wallet.enums.TransactionStatus;
import com.hesap.wallet.enums.TransactionType;
import com.hesap.wallet.exception.AccountNotFoundException;
import com.hesap.wallet.exception.InsufficientBalanceException;
import com.hesap.wallet.exception.InvalidTransferException;
import com.hesap.wallet.factory.TransactionFactory;
import com.hesap.wallet.repository.AccountRepository;
import com.hesap.wallet.repository.TransactionRepository;
import com.hesap.wallet.service.transfer.validation.DistinctAccountsValidator;
import com.hesap.wallet.service.transfer.validation.PositiveAmountValidator;
import com.hesap.wallet.service.transfer.validation.SameCurrencyValidator;
import com.hesap.wallet.service.transfer.validation.SufficientBalanceValidator;
import com.hesap.wallet.service.transfer.validation.TransferValidationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalTransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private InternalTransferService service;

    @BeforeEach
    void setUp() {
        // Real validators + real factory: the engine wiring is what we want to exercise.
        List<TransferValidationStrategy> validators = List.of(
                new PositiveAmountValidator(),
                new DistinctAccountsValidator(),
                new SameCurrencyValidator(),
                new SufficientBalanceValidator());
        service = new InternalTransferService(
                accountRepository, transactionRepository, new TransactionFactory(), validators);
    }

    private static Account account(long id, Currency currency, String balance) {
        return Account.builder().id(id).userId(id).currency(currency).balance(new BigDecimal(balance)).build();
    }

    @Test
    void transfer_movesFundsAndWritesLinkedLedgerEntries() {
        Account sender = account(1L, Currency.TRY, "100.00");
        Account receiver = account(2L, Currency.TRY, "20.00");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));
        AtomicLong idSeq = new AtomicLong(10);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(idSeq.getAndIncrement());
            t.setCreatedAt(Instant.parse("2026-06-16T00:00:00Z"));
            return t;
        });

        TransferResponse response = service.transfer(new TransferRequest(1L, 2L, new BigDecimal("30.00")));

        // Balances moved.
        assertThat(sender.getBalance()).isEqualByComparingTo("70.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("50.00");

        // Two linked ledger legs, OUT then IN.
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        Transaction out = captor.getAllValues().get(0);
        Transaction in = captor.getAllValues().get(1);

        assertThat(out.getType()).isEqualTo(TransactionType.TRANSFER_OUT);
        assertThat(out.getAccount()).isSameAs(sender);
        assertThat(out.getAmount()).isEqualByComparingTo("30.00");
        assertThat(out.getBalanceAfter()).isEqualByComparingTo("70.00");
        assertThat(out.getRelatedAccountId()).isEqualTo(2L);

        assertThat(in.getType()).isEqualTo(TransactionType.TRANSFER_IN);
        assertThat(in.getAccount()).isSameAs(receiver);
        assertThat(in.getBalanceAfter()).isEqualByComparingTo("50.00");
        assertThat(in.getRelatedAccountId()).isEqualTo(1L);

        assertThat(out.getTransferReference()).isNotNull().isEqualTo(in.getTransferReference());

        // Response is built from the OUT leg.
        assertThat(response.transactionId()).isEqualTo(out.getId());
        assertThat(response.status()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(response.timestamp()).isEqualTo(out.getCreatedAt());
    }

    @Test
    void transfer_insufficientFunds_checkedUnderLock_noMutationOrLedger() {
        Account sender = account(1L, Currency.TRY, "10.00");
        Account receiver = account(2L, Currency.TRY, "0.00");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> service.transfer(new TransferRequest(1L, 2L, new BigDecimal("50.00"))))
                .isInstanceOf(InsufficientBalanceException.class);

        // Locks were taken (funds checked under lock) ...
        verify(accountRepository).findByIdForUpdate(1L);
        verify(accountRepository).findByIdForUpdate(2L);
        // ... but nothing was mutated or recorded.
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        assertThat(sender.getBalance()).isEqualByComparingTo("10.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void transfer_sameAccount_failsInStep1_beforeAnyLock() {
        assertThatThrownBy(() -> service.transfer(new TransferRequest(5L, 5L, new BigDecimal("10.00"))))
                .isInstanceOf(InvalidTransferException.class);

        // Template order proof: step 1 short-circuits, so no lock/update/ledger happens.
        verify(accountRepository, never()).findByIdForUpdate(anyLong());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_senderNotFound_throwsAndWritesNothing() {
        when(accountRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(new TransferRequest(1L, 2L, new BigDecimal("10.00"))))
                .isInstanceOf(AccountNotFoundException.class);

        verify(transactionRepository, never()).save(any());
    }
}
