package com.back.together02be.asset.service;

import com.back.together02be.asset.dto.response.UserStockRes;
import com.back.together02be.asset.entity.UserStock;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.stock.service.StockService;
import org.springframework.stereotype.Service;

import com.back.together02be.asset.dto.response.StockInfoRes;
import com.back.together02be.asset.entity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AssetService {
    private final UserAccountRepository userAccountRepository;
    private final UserStockRepository userStockRepository;
    private final StockService stockService;

    public List<UserStockRes> getUserStocks(Long userId) {
        //보유 주식 목록 조회
        List<UserStock> userStocks = userStockRepository.findAllByUsersId(userId);
        return getUserStocksRealtimePrice(userStocks);
    }

    // 유저 보유 종목 현재 시세 조회
    public List<UserStockRes> getUserStocksRealtimePrice(List<UserStock> userStocks) {

        return userStocks.stream().map(userStock -> {
            String stockCode = userStock.getStock().getStockCode();
            Long currentPrice = stockService.getCachedStockPrice(stockCode).currentPrice();


            return UserStockRes.from(userStock, currentPrice);
        }).collect(Collectors.toList());
    }
    public long getTotalAmountByUserId(long userId){
        return userAccountRepository.findByUsersId(userId)
                .orElseThrow(()->new RuntimeException("계좌 없음"))
                .getTotalPurchase();
    }
    public List<StockInfoRes> getStockInfo(long userId){
        List<UserStock> userStocks = userStockRepository.findAllByUsersId(userId);

        List<StockInfoRes> stockInfos = userStocks.stream()
                .map(us->new StockInfoRes(us.getStock().getStockCode(),us.getQuantity()))
                .toList();

        return stockInfos;
    }
}
