package com.back.together02be.asset.service;

import com.back.together02be.asset.dto.response.UserStockRes;
import com.back.together02be.asset.repository.UserStockRepository;
import org.springframework.stereotype.Service;

import com.back.together02be.asset.dto.response.StockInfoRes;
import com.back.together02be.asset.entity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final UserAccountRepository userAccountRepository;
    private final UserStockRepository userStockRepository;

    public List<UserStockRes> getUserStocks(Long userId) {
        return userStockRepository.findAllByUsersId(userId).stream()
                .map(UserStockRes::from)
                .toList();
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
