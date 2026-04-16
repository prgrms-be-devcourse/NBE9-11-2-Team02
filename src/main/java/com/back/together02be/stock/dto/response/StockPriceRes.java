package com.back.together02be.stock.dto.response;

import com.back.together02be.stock.entity.Stock;

public record StockPriceRes(
	String stockCode,
	String stockName
) {
	public static StockPriceRes from(Stock stock) {
		return new StockPriceRes(
			stock.getStockCode(),
			stock.getStockName()
		);
	}
}
