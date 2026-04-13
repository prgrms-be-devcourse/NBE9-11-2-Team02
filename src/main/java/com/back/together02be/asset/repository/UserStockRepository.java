package com.back.together02be.asset.repository;

import com.back.together02be.asset.entity.UserStock;
import org.springframework.data.jpa.repository.JpaRepository;

import com.back.together02be.asset.entity.UserAccount;

import java.util.List;

public interface UserStockRepository extends JpaRepository<UserStock, Long> {
    public List<UserStock> findAllByUsersId(Long userId);
}
