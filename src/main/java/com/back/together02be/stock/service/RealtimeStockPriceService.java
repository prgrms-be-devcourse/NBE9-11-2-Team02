package com.back.together02be.stock.service;

import com.back.together02be.stock.enitity.RealtimeStockPrice;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RealtimeStockPriceService {

    private final ConcurrentHashMap<String, RealtimeStockPrice> priceMap = new ConcurrentHashMap<>();

    public void put(String stockCode, RealtimeStockPrice stockPrice) {
        this.priceMap.put(stockCode,stockPrice);
    }

    public RealtimeStockPrice get(String stockCode) {
        return this.priceMap.get(stockCode);
    }
}
