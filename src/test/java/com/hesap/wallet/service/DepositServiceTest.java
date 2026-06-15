package com.hesap.wallet.service;

import com.hesap.wallet.dto.request.DepositRequest;
import com.hesap.wallet.dto.response.DepositResponse;
import com.hesap.wallet.entity.Account;
import com.hesap.wallet.entity.Transaction;
import com.hesap.wallet.enums.Currency;
import com.hesap.wallet.enums.TransactionStatus;
import com.hesap.wallet.enums.TransactionType;
import com.hesap.wallet.exception.AccountNotFoundException;
import com.hesap.wallet.factory.TransactionFactory;
import com.hesap.wallet.repository.AccountRepository;
import com.hesap.wallet.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    // Real factory: a pure builder with no collaborators worth mocking.
    private final TransactionFactory transactionFactory = new TransactionFactory();

    private DepositService depositService;

    @BeforeEach
    void setUp() {
        depositService = new DepositService(accountRepository, transactionRepository, transactionFactory);
    }

    @Test
    void deposit_increasesBalanceAndWritesDepositLedgerEntry() {
        Account account = Account.builder()
                .id(1L)
                .userId(42L)
                .currency(Currency.TRY)
                .balance(new BigDecimal("100.00"))
                .build();
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });

        DepositResponse response = depositService.deposit(1L, new DepositRequest(new BigDecimal("50.00")));

        // Balance incremented.
        assertThat(account.getBalance()).isEqualByComparingTo("150.00");

        // Response reflects the persisted ledger entry.
        assertThat(response.transactionId()).isEqualTo(99L);
        assertThat(response.accountId()).isEqualTo(1L);
        assertThat(response.newBalance()).isEqualByComparingTo("150.00");
        assertThat(response.status()).isEqualTo(TransactionStatus.SUCCESS);

        // A DEPOSIT ledger entry was written with the post-deposit balance snapshot.
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction ledger = captor.getValue();
        assertThat(ledger.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(ledger.getAmount()).isEqualByComparingTo("50.00");
        assertThat(ledger.getBalanceAfter()).isEqualByComparingTo("150.00");
        assertThat(ledger.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(ledger.getRelatedAccountId()).isNull();
        assertThat(ledger.getTransferReference()).isNull();
    }

    @Test
    void deposit_throwsWhenAccountNotFound_andWritesNoLedgerEntry() {
        when(accountRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> depositService.deposit(999L, new DepositRequest(new BigDecimal("10.00"))))
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
