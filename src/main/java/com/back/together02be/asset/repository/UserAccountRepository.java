package com.back.together02be.asset.repository;

import com.back.together02be.asset.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
	Optional<UserAccount> findByUsersId(Long usersId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT u FROM UserAccount u WHERE u.users.id = :userId")
	Optional<UserAccount> findByUsersIdWithLock(@Param("userId") Long userId);

	@Modifying(clearAutomatically = true)
	@Query("UPDATE UserAccount u SET u.deposit = u.deposit - :amount, u.totalPurchase = u.totalPurchase + :amount " +
		"WHERE u.users.id = :userId AND u.deposit >= :amount")
	int decreaseDepositIfSufficient(@Param("userId") Long userId, @Param("amount") Long amount);
}
