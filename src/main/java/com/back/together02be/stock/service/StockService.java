package com.back.together02be.stock.service;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.back.together02be.infra.kis.KisWebSocketClient;
import com.back.together02be.stock.dto.RealtimeStockPrice;
import com.back.together02be.stock.repository.StockRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

	private final RealTimeStockPriceStore rtStockPriceStore;
	private final KisWebSocketClient kisWebSocketClient;
	private final StockRepository stockRepository;

	public SseEmitter createSseEmitter(String stockCode) {

		stockRepository.findByStockCode(stockCode)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 종목코드입니다: " + stockCode));

		int count = rtStockPriceStore.addSubscriber(stockCode); // 구독자 추가

		// 종목의 첫 구독자일 때만 구독 요청
		if (count == 1) {
			kisWebSocketClient.subscribe(stockCode);
		}

		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(() -> { // (작업, 초기 지연, 주기, 단위)
			try {
				RealtimeStockPrice stockPrice = rtStockPriceStore.get(stockCode);
				if (stockPrice != null) {
					emitter.send(stockPrice);
				}

			} catch (IOException e) {
				emitter.complete();
				executor.shutdown();
			} catch (Exception e) {
				log.error("SSE 오류 - 종목: {} | 원인: {}", stockCode, e.getMessage(), e);
				emitter.complete();
				executor.shutdown();
			}
		}, 0, 1, TimeUnit.SECONDS);

		emitter.onCompletion(() -> {
			executor.shutdown();
			int remaining = rtStockPriceStore.removeSubscriber(stockCode);
			if (remaining <= 0) {
				kisWebSocketClient.unsubscribe(stockCode);  // 마지막 구독자일 때만 취소
			}
		});

		emitter.onTimeout(() -> {
			executor.shutdown();
			int remaining = rtStockPriceStore.removeSubscriber(stockCode);
			if (remaining <= 0) {
				kisWebSocketClient.unsubscribe(stockCode);
			}
			log.info("SSE 타임아웃 - 종목: {}", stockCode);
		});

		return emitter;
	}
}
