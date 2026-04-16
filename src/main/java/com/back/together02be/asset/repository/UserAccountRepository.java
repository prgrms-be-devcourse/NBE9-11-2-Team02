package com.back.together02be.asset.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.together02be.asset.enitity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
	Optional<UserAccount> findByUsersId(Long usersId);
}
