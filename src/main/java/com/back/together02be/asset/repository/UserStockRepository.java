package com.back.together02be.asset.repository;

import com.back.together02be.asset.entity.UserStock;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserStockRepository extends JpaRepository<UserStock, Long> {
    //비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(
            name = "jakarta.persistence.lock.timeout",
            value = "3000" //락 타임아웃(3초)
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
    void delete(UserStock userStock);

    Optional<UserStock> findByUsersIdAndStockId(
            Long userId,
            Long stockId
    );
}
