package com.hesap.wallet.service;

import com.hesap.wallet.dto.request.DepositRequest;
import com.hesap.wallet.dto.response.DepositResponse;
import com.hesap.wallet.entity.Account;
import com.hesap.wallet.entity.Transaction;
import com.hesap.wallet.exception.AccountNotFoundException;
import com.hesap.wallet.factory.TransactionFactory;
import com.hesap.wallet.repository.AccountRepository;
import com.hesap.wallet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles balance top-ups. Loads the account under a pessimistic write lock, increments
 * the balance, and records a DEPOSIT ledger entry atomically; any failure rolls back both
 * writes. The lock serializes concurrent deposits on the same account (no lost updates).
 */
@Service
@RequiredArgsConstructor
public class DepositService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionFactory transactionFactory;

    @Transactional
    public DepositResponse deposit(Long accountId, DepositRequest request) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        account.setBalance(account.getBalance().add(request.amount()));
        accountRepository.save(account);

        Transaction ledgerEntry = transactionFactory.deposit(account, request.amount());
        Transaction saved = transactionRepository.save(ledgerEntry);

        return DepositResponse.from(saved);
    }
}
