package com.back.together02be.stock.controller;

import com.back.together02be.stock.client.KisPriceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StockTestController {

    private final KisPriceClient kisPriceClient;

    @GetMapping("/api/test/token")
    public String testToken() {
        return kisPriceClient.getAccessToken();
    }
}