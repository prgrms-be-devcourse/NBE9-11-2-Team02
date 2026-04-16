package com.back.together02be.asset.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.together02be.asset.enitity.UserStock;

public interface UserStockRepository extends JpaRepository<UserStock, Long> {
	Optional<UserStock> findByUsersIdAndStockId(Long usersId, Long stockId);
}
