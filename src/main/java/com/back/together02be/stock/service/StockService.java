package com.back.together02be.stock.service;

import com.back.together02be.stock.client.KisPriceClient;
import com.back.together02be.stock.dto.KisPriceResponse;
import com.back.together02be.stock.dto.StockListResponse;
import com.back.together02be.stock.entity.Stock;
import com.back.together02be.stock.repository.StockRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final KisPriceClient kisPriceClient;

    public List<StockListResponse> getStocks() {
        String token = kisPriceClient.getAccessToken();
        List<StockListResponse> result = new ArrayList<>();

        for (Stock stock : stockRepository.findByIsActiveTrue()) {
            KisPriceResponse price = kisPriceClient.getCurrentPrice(token, stock.getStockCode());

            result.add(new StockListResponse(
                    stock.getId(),
                    stock.getStockCode(),
                    stock.getStockName(),
                    stock.getMarket(),
                    Long.parseLong(price.output().stck_prpr()),
                    Double.parseDouble(price.output().prdy_ctrt())
            ));
        }

        return result;
    }
}
