package com.back.together02be.stock.cache;

public record StockPriceCache(
        Long currentPrice,
        Double changeRate
) {
}