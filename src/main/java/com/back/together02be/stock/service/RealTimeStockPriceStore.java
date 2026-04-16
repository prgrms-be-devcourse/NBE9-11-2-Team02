package com.back.together02be.stock.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.back.together02be.stock.dto.RealtimeStockPrice;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RealTimeStockPriceStore {

	private final ConcurrentHashMap<String, RealtimeStockPrice> priceMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, AtomicInteger> subscriberCount = new ConcurrentHashMap<>();

	// 실시간 시세 넣기
	public void put(String stockCode, RealtimeStockPrice stockPrice) {
		priceMap.put(stockCode, stockPrice);
	}

	// 실시간 시세 꺼내기
	public RealtimeStockPrice get(String stockCode) {
		return priceMap.get(stockCode);
	}

	// 구독자 추가
	public int addSubscriber(String stockCode) {
		int count = subscriberCount.computeIfAbsent(stockCode, k -> new AtomicInteger(0)).incrementAndGet();
		// log.info("구독자 추가 - 종목: {} | 현재 구독자 수: {}", stockCode, count);
		return count;
	}

	// 구독자 제거-> 남은 구독자 수 반환
	public int removeSubscriber(String stockCode) {
		AtomicInteger count = subscriberCount.get(stockCode);
		if (count == null) return 0;
		int remaining = count.decrementAndGet();
		// log.info("구독자 제거 - 종목: {} | 남은 구독자 수: {}", stockCode, remaining);
		return remaining;
	}
}
