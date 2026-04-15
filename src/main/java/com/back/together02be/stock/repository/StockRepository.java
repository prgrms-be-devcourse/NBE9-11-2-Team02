package com.back.together02be.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.together02be.stock.entity.Stock;

public interface StockRepository extends JpaRepository<Stock, Long> {
	Optional<Stock> findByStockCode(String stockCode);
}
