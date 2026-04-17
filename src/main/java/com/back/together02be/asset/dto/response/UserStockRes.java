package com.back.together02be.asset.dto.response;

import com.back.together02be.asset.entity.UserStock;

public record UserStockRes(
        String stockCode,
        String stockName,
        Long quantity,
        Long averagePrice
) {
    public static UserStockRes from(UserStock userStock) {
        return new UserStockRes(
                userStock.getStock().getStockCode(),
                userStock.getStock().getStockName(),
                userStock.getQuantity(),
                userStock.getAveragePrice()
        );
    }
}