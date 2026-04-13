package com.back.together02be.asset.service;

import com.back.together02be.asset.controller.AssetController;
import com.back.together02be.asset.dto.response.StockInfoRes;
import com.back.together02be.asset.entity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final UserAccountRepository userAccountRepository;
    private final UserStockRepository userStockRepository;
    public long getTotalAmountByUserId(long userId){
        return userAccountRepository.findById(userId)
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
