package com.back.together02be.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.together02be.stock.entity.Stock;

import java.util.List;

public interface StockRepository extends JpaRepository<Stock, Long> {

    List<Stock> findByIsActiveTrue();

}
