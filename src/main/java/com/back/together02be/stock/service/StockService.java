package com.back.together02be.stock.service;

import com.back.together02be.stock.dto.StockListResponse;
import com.back.together02be.stock.repository.StockRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    public List<StockListResponse> getStocks() {
        return stockRepository.findByIsActiveTrue()
                .stream()
                .map(stock -> new StockListResponse(
                        stock.getId(),
                        stock.getStockCode(),
                        stock.getStockName(),
                        stock.getMarket(),
                        null,
                        null,
                        null
                ))
                .toList();
    }
}
