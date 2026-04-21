package com.back.together02be.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.back.together02be.asset.entity.UserAccount;
import com.back.together02be.asset.entity.UserStock;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserStockRepository extends JpaRepository<UserStock, Long> {
	Optional<UserStock> findByUsersIdAndStockId(Long usersId, Long stockId);
    List<UserStock> findAllByUsersId(Long users_id);

    //비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(
            name = "jakarta.persistence.lock.timeout",
            value = "500" //락 타임아웃(0.5초)
    ))
    @Query("""
        SELECT us FROM UserStock us
        WHERE us.users.id = :usersId
        AND us.stock.id = :stockId
    """)
    Optional<UserStock> findByUsersIdAndStockIdWithLock( //userId, stockId로 주식 정보 찾기
            @Param("usersId") Long usersId,
            @Param("stockId") Long stockId
    );

    // 더티 체킹을 사용하지 않으므로, 연산 후 영속성 컨텍스트를 비워줘야 최신 데이터가 반영됩니다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserStock u SET u.quantity = u.quantity - :sellQuantity " +
            "WHERE u.users.id = :userId AND u.stock.id = :stockId AND u.quantity >= :sellQuantity")
    int updateQuantity(@Param("userId") Long userId,
                       @Param("stockId") Long stockId,
                       @Param("sellQuantity") Long sellQuantity);

    void delete(UserStock userStock);
}
