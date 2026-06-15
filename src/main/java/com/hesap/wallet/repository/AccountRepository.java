package com.hesap.wallet.repository;

import com.hesap.wallet.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access for {@link Account}.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Loads an account while holding a {@code PESSIMISTIC_WRITE} (SELECT ... FOR UPDATE)
     * row lock. This serializes concurrent balance mutations on the same account,
     * preventing lost-update race conditions. Must be called inside a transaction; the
     * lock is held until the surrounding transaction commits or rolls back.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
