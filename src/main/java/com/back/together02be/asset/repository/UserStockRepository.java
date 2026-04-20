package com.back.together02be.asset.repository;

import com.back.together02be.asset.entity.UserStock;
import org.springframework.data.jpa.repository.JpaRepository;
import com.back.together02be.asset.entity.UserAccount;
import java.util.List;
import java.util.Optional;

public interface UserStockRepository extends JpaRepository<UserStock, Long> {
	Optional<UserStock> findByUsersIdAndStockId(Long usersId, Long stockId);
    List<UserStock> findAllByUsersId(Long users_id);
}
