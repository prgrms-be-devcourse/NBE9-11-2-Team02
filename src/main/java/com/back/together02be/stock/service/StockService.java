package com.back.together02be.stock.service;

import com.back.together02be.stock.cache.StockPriceCache;
import com.back.together02be.stock.client.KisPriceClient;
import com.back.together02be.stock.dto.KisPriceRes;
import com.back.together02be.stock.dto.StockListRes;
import com.back.together02be.stock.entity.Stock;
import com.back.together02be.stock.repository.StockRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final KisPriceClient kisPriceClient;

    private final Map<String, StockPriceCache> priceCache = new ConcurrentHashMap<>();

    private int currentIndex = 0;

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
                    stock.getMarket(),
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

        if (stocks.isEmpty()) return;

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
}
