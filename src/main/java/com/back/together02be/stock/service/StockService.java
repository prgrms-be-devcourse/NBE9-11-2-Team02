package com.back.together02be.stock.service;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.back.together02be.infra.kis.KisWebSocketClient;
import com.back.together02be.stock.dto.RealtimeStockPrice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

	private final RealTimeStockPriceStore rtStockPriceStore;
	private final KisWebSocketClient kisWebSocketClient;

	public SseEmitter createSseEmitter(String stockCode) {
		kisWebSocketClient.subscribe(stockCode); // 구독 요청

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
				emitter.completeWithError(e);
				executor.shutdown();
			}
		}, 0, 1, TimeUnit.SECONDS);

		emitter.onCompletion(() -> {
			log.info("✅ SSE 연결 끊김 - 종목: {}", stockCode);  // ← 로그 추가
			executor.shutdown();
			kisWebSocketClient.unsubscribe(stockCode);
		});

		emitter.onTimeout(() -> {
			executor.shutdown();
			log.info("SSE 타임아웃 - 종목: {}", stockCode);
		});

		return emitter;
	}
}
