package com.hesap.wallet.service;

import com.hesap.wallet.dto.response.TransactionResponse;
import com.hesap.wallet.entity.Transaction;
import com.hesap.wallet.enums.TransactionType;
import com.hesap.wallet.exception.AccountNotFoundException;
import com.hesap.wallet.repository.AccountRepository;
import com.hesap.wallet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read side for an account's ledger: returns transaction history newest-first, optionally
 * filtered by {@link TransactionType}.
 */
@Service
@RequiredArgsConstructor
public class TransactionHistoryService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<TransactionResponse> getHistory(Long accountId, TransactionType type) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        List<Transaction> transactions = (type == null)
                ? transactionRepository.findByAccount_IdOrderByCreatedAtDesc(accountId)
                : transactionRepository.findByAccount_IdAndTypeOrderByCreatedAtDesc(accountId, type);

        return transactions.stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
