package com.back.together02be.trade.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;

import com.back.together02be.trade.enitity.Trade;
import org.springframework.data.jpa.repository.Lock;

public interface TradeRepository extends JpaRepository<Trade, Long> {
}
