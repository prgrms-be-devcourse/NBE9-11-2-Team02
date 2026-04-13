package com.back.together02be.stock.service;

import com.back.together02be.stock.client.KisPriceClient;
import com.back.together02be.stock.dto.KisPriceResponse;
import com.back.together02be.stock.dto.StockListResponse;
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

    private record StockPriceCache(
            Long currentPrice,
            Double changeRate
    ) {
    }

    public List<StockListResponse> getStocks() {
        String token = kisPriceClient.getAccessToken();
        List<StockListResponse> result = new ArrayList<>();

        for (Stock stock : stockRepository.findByIsActiveTrue()) {

            StockPriceCache cached = priceCache.get(stock.getStockCode());

            Long currentPrice;
            Double changeRate;

            if (cached != null) {
                // 캐시 사용
                currentPrice = cached.currentPrice();
                changeRate = cached.changeRate();
            } else {
                // 없으면 API 호출
                KisPriceResponse price = kisPriceClient.getCurrentPrice(token, stock.getStockCode());

                currentPrice = Long.parseLong(price.output().stck_prpr());
                changeRate = Double.parseDouble(price.output().prdy_ctrt());

                // 캐시에 저장
                priceCache.put(stock.getStockCode(), new StockPriceCache(currentPrice, changeRate));
            }

            result.add(new StockListResponse(
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

    //5초마다 갱신
    @Scheduled(fixedRate = 5000)
    public void updatePriceCache() {
        String token = kisPriceClient.getAccessToken();

        for (Stock stock : stockRepository.findByIsActiveTrue()) {

            try {
                KisPriceResponse price = kisPriceClient.getCurrentPrice(token, stock.getStockCode());

                Long currentPrice = Long.parseLong(price.output().stck_prpr());
                Double changeRate = Double.parseDouble(price.output().prdy_ctrt());

                priceCache.put(stock.getStockCode(), new StockPriceCache(currentPrice, changeRate));

            } catch (Exception e) {
                System.out.println("가격 갱신 실패: " + stock.getStockCode());
            }
        }
    }
}
