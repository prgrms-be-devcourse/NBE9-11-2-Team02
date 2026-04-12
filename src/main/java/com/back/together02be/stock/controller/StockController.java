package com.back.together02be.stock.controller;

import com.back.together02be.stock.dto.StockListResponse;
import com.back.together02be.stock.service.StockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
@Tag(name = "StockController", description = "주식 API")
public class StockController {

    private final StockService stockService;

    @GetMapping
    public List<StockListResponse> getStocks() {
        return stockService.getStocks();
    }
}
