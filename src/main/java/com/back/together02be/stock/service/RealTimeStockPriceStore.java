package com.back.together02be.stock.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.back.together02be.stock.dto.RealtimeStockPrice;

@Service
public class RealTimeStockPriceStore {

	private final ConcurrentHashMap<String, RealtimeStockPrice> priceMap = new ConcurrentHashMap<>();

	public void put(String stockCode, RealtimeStockPrice stockPrice) {
		priceMap.put(stockCode, stockPrice);
	}

	public RealtimeStockPrice get(String stockCode) {
		return priceMap.get(stockCode);
	}
}
