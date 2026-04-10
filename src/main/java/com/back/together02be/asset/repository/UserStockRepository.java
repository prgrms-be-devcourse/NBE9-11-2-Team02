package com.back.together02be.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.together02be.asset.enitity.UserAccount;

public interface UserStockRepository extends JpaRepository<UserAccount, Long> {
}
