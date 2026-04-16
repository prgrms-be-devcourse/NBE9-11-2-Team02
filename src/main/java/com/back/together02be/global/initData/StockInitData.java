package com.back.together02be.global.initData;

import com.back.together02be.stock.entity.Stock;
import com.back.together02be.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class StockInitData {

    private final StockRepository stockRepository;

    @Bean
    public ApplicationRunner initStocks() {
        return args -> initStockData();
    }

    @Transactional
    public void initStockData() {
        if (stockRepository.count() > 0) return;

        stockRepository.save(new Stock("005930", "삼성전자", "KOSPI"));
        stockRepository.save(new Stock("000660", "SK하이닉스", "KOSPI"));
        stockRepository.save(new Stock("035420", "NAVER", "KOSPI"));
        stockRepository.save(new Stock("035720", "카카오", "KOSPI"));
        stockRepository.save(new Stock("068270", "셀트리온", "KOSPI"));
        stockRepository.save(new Stock("005380", "현대차", "KOSPI"));
        stockRepository.save(new Stock("012330", "현대모비스", "KOSPI"));
        stockRepository.save(new Stock("105560", "KB금융", "KOSPI"));
        stockRepository.save(new Stock("055550", "신한지주", "KOSPI"));
        stockRepository.save(new Stock("034730", "SK", "KOSPI"));
        stockRepository.save(new Stock("066570", "LG전자", "KOSPI"));
        stockRepository.save(new Stock("003670", "포스코퓨처엠", "KOSPI"));
        stockRepository.save(new Stock("096770", "SK이노베이션", "KOSPI"));
        stockRepository.save(new Stock("015760", "한국전력", "KOSPI"));
        stockRepository.save(new Stock("032830", "삼성생명", "KOSPI"));
        stockRepository.save(new Stock("086790", "하나금융지주", "KOSPI"));
        stockRepository.save(new Stock("051910", "LG화학", "KOSPI"));
        stockRepository.save(new Stock("006400", "삼성SDI", "KOSPI"));
        stockRepository.save(new Stock("207940", "삼성바이오로직스", "KOSPI"));
        stockRepository.save(new Stock("373220", "LG에너지솔루션", "KOSPI"));
    }
}