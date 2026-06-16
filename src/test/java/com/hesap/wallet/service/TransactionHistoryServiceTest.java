package com.hesap.wallet.service;

import com.hesap.wallet.dto.response.TransactionResponse;
import com.hesap.wallet.entity.Transaction;
import com.hesap.wallet.enums.TransactionStatus;
import com.hesap.wallet.enums.TransactionType;
import com.hesap.wallet.exception.AccountNotFoundException;
import com.hesap.wallet.repository.AccountRepository;
import com.hesap.wallet.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionHistoryServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionHistoryService service;

    private static Transaction deposit() {
        return Transaction.builder()
                .id(1L)
                .type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("10.00"))
                .balanceAfter(new BigDecimal("10.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(Instant.parse("2026-06-16T00:00:00Z"))
                .build();
    }

    @Test
    void getHistory_withoutFilter_usesUnfilteredQueryAndMaps() {
        when(accountRepository.existsById(1L)).thenReturn(true);
        when(transactionRepository.findByAccount_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(deposit()));

        List<TransactionResponse> result = service.getHistory(1L, null);

        assertThat(result).singleElement().satisfies(tx -> {
            assertThat(tx.transactionId()).isEqualTo(1L);
            assertThat(tx.type()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(tx.balanceAfter()).isEqualByComparingTo("10.00");
        });
        verify(transactionRepository).findByAccount_IdOrderByCreatedAtDesc(1L);
        verify(transactionRepository, never()).findByAccount_IdAndTypeOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void getHistory_withFilter_usesTypedQuery() {
        when(accountRepository.existsById(1L)).thenReturn(true);
        when(transactionRepository.findByAccount_IdAndTypeOrderByCreatedAtDesc(1L, TransactionType.DEPOSIT))
                .thenReturn(List.of(deposit()));

        service.getHistory(1L, TransactionType.DEPOSIT);

        verify(transactionRepository).findByAccount_IdAndTypeOrderByCreatedAtDesc(1L, TransactionType.DEPOSIT);
        verify(transactionRepository, never()).findByAccount_IdOrderByCreatedAtDesc(any());
    }

    @Test
    void getHistory_unknownAccount_throwsAndTouchesNoLedger() {
        when(accountRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getHistory(999L, null))
                .isInstanceOf(AccountNotFoundException.class);

        verifyNoInteractions(transactionRepository);
    }
}
