package com.back.together02be.stock.controller;

import com.back.together02be.stock.dto.StockListRes;
import com.back.together02be.stock.service.StockService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
@Tag(name = "StockController", description = "주식 API")
public class StockController {

    private final StockService stockService;

    @GetMapping
    public List<StockListRes> getStocks() {
        return stockService.getStocks();
    }

	@GetMapping(value = "/{stockCode}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamStockPrice(@PathVariable String stockCode) {
		return stockService.createSseEmitter(stockCode);
	}
}
