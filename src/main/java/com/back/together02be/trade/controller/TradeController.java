package com.back.together02be.trade.controller;

import com.back.together02be.global.apiRes.ApiRes;
import com.back.together02be.trade.service.TradeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trades")
@Tag(name = "TradeController", description = "주식 거래 API")
public class TradeController {
    private final TradeService tradeService;

    public record TradeSellReq(
            Long userId,
            Long stockId,
            @Min(value = 1, message = "매도 수량은 최소 1개여야 합니다.")
            Long quantity,
            @Min(value = 1, message = "가격은 0이하일 수 없습니다. ")
            Long price
    ){ }

    @PostMapping("/sell")
    public ApiRes<String> sell(@RequestBody @Valid TradeSellReq req){
        tradeService.sell(req);

        return new ApiRes<String>(
                "매도 주문이 성공적으로 체결되었습니다.",
                "SUCCESS"
        );
    }
}
