package com.back.together02be.asset.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;

import com.back.together02be.asset.entity.UserAccount;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    //비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(
            name = "jakarta.persistence.lock.timeout",
            value = "3000" //타임아웃 3초
    ))
    @Query("SELECT ua FROM UserAccount ua WHERE ua.users.id = :usersId")
    Optional<UserAccount> findByUsersIdWithLock(@Param("usersId") Long usersId);

    Optional<UserAccount> findByUsersId(Long usersId);
}
