package com.hesap.wallet.repository;

import com.hesap.wallet.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for {@link Account}. A pessimistic-write lookup used by the transfer
 * engine is added in the transfer phase.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
}
