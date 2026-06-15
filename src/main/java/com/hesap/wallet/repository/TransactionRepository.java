package com.hesap.wallet.repository;

import com.hesap.wallet.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for {@link Transaction} ledger entries. History queries with filtering
 * and date ordering are added in the history phase.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
