package com.back.together02be.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.together02be.stock.enitity.Stock;

public interface StockRepository extends JpaRepository<Stock, Long> {
}
