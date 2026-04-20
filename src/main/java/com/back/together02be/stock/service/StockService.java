package com.back.together02be.stock.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.back.together02be.infra.kis.KisWebSocketClient;
import com.back.together02be.stock.cache.StockPriceCache;
import com.back.together02be.stock.client.KisPriceClient;
import com.back.together02be.stock.dto.response.KisPriceRes;
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
	private final KisWebSocketClient kisWebSocketClient;

	// REST 전체 종목 조회용 캐시
	private final Map<String, StockPriceCache> priceCache = new ConcurrentHashMap<>();

	private int currentIndex = 0;

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

	//스케줄러 메서드
	@Scheduled(fixedDelay = 1000) // 1초마다
	public void updatePriceCache() {

		List<Stock> stocks = stockRepository.findAll();

		if (stocks.isEmpty())
			return;

		String token = kisPriceClient.getAccessToken();

		// 한투 OpenAPI 호출 제한 때문에 전체 종목을 한 번에 갱신하지 않고 순차 갱신
		int index = currentIndex % stocks.size();
		Stock stock = stocks.get(index);

		try {
			KisPriceRes price = kisPriceClient.getCurrentPrice(token, stock.getStockCode());

			Long currentPrice = Long.parseLong(price.output().currentPrice());
			Double changeRate = Double.parseDouble(price.output().changeRate());

			priceCache.put(stock.getStockCode(),
				new StockPriceCache(currentPrice, changeRate));

		} catch (Exception e) {
			System.out.println("가격 갱신 실패: " + stock.getStockCode() + " / " + e.getMessage());
		}

		// 다음 위치로 이동
		currentIndex = (currentIndex + 1) % stocks.size();
	}


	public SseEmitter createSseEmitter(String stockCode) {

		findStock(stockCode);

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

	public StockPriceRes getStockPrice(String stockCode) {
		return StockPriceRes.from(findStock(stockCode));
	}

	private Stock findStock(String stockCode) {
		return stockRepository.findByStockCode(stockCode)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 종목코드입니다: " + stockCode));
	}
}
