package com.back.together02be.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.together02be.asset.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
}
