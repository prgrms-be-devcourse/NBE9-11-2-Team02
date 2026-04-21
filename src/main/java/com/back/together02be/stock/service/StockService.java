package com.back.together02be.stock.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.back.together02be.stock.cache.StockPriceCache;
import com.back.together02be.stock.client.KisPriceClient;
import com.back.together02be.stock.dto.RealtimeStockPrice;
import com.back.together02be.stock.dto.response.StockListRes;
import com.back.together02be.stock.dto.response.StockPriceRes;
import com.back.together02be.stock.entity.Stock;
import com.back.together02be.stock.repository.StockRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

	private final StockRepository stockRepository;
	private final KisPriceClient kisPriceClient;

	//SSE용
	private final RealTimeStockPriceStore rtStockPriceStore;

	private static final long SSE_INTERVAL_MS = 500; // 상세 종목 시세 갱신 주기

	// REST 전체 종목 조회용 캐시
	private final Map<String, StockPriceCache> priceCache = new ConcurrentHashMap<>();

	//캐시 읽는 메서드
	public StockPriceCache getCachedStockPrice(String stockCode) {
		return priceCache.get(stockCode);
	}

    //캐시 읽는 메서드
    public List<StockListRes> getStocks() {
        List<StockListRes> result = new ArrayList<>();

		for (Stock stock : stockRepository.findAll()) {
			StockPriceCache cached = priceCache.get(stock.getStockCode());

			Long currentPrice = null;
			Double changeRate = null;

			if (cached != null) {
				currentPrice = cached.currentPrice();
				changeRate = cached.changeRate();
			}

			result.add(new StockListRes(
				stock.getId(),
				stock.getStockCode(),
				stock.getStockName(),
				currentPrice,
				changeRate
			));
		}

		return result;
	}

	public SseEmitter createSseEmitter(String stockCode) {

		findStock(stockCode);

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
		}, 0, SSE_INTERVAL_MS, TimeUnit.MILLISECONDS);

		emitter.onCompletion(() -> executor.shutdown());

		emitter.onTimeout(() -> {
			executor.shutdown();
			log.info("SSE 타임아웃 - 종목: {}", stockCode);
		});

		return emitter;
	}

	public StockPriceRes getStockPrice(String stockCode) {
		return StockPriceRes.from(findStock(stockCode));
	}

	private Stock findStock(String stockCode) {
		return stockRepository.findByStockCode(stockCode)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 종목코드입니다: " + stockCode));
	}
}
