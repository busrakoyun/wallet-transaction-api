package com.hesap.wallet.repository;

import com.hesap.wallet.entity.Transaction;
import com.hesap.wallet.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access for {@link Transaction} ledger entries. History queries return entries for an
 * account ordered newest-first, optionally filtered by type.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccount_IdOrderByCreatedAtDesc(Long accountId);

    List<Transaction> findByAccount_IdAndTypeOrderByCreatedAtDesc(Long accountId, TransactionType type);
}
