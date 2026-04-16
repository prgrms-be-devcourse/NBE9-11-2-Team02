package com.back.together02be.trade.controller;

import com.back.together02be.global.apiRes.ApiRes;
import com.back.together02be.trade.dto.request.TradeSellReq;
import com.back.together02be.trade.service.TradeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import com.back.together02be.global.apiRes.ApiRes;
import com.back.together02be.trade.dto.BuyReq;
import com.back.together02be.trade.dto.BuyRes;
import com.back.together02be.trade.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trades")
@Tag(name = "TradeController", description = "주식 거래 API")
public class TradeController {

    private final TradeService tradeService;

    /**
     * TR-01 매수
     * TODO: Spring Security 적용 후 @AuthenticationPrincipal로 userId 주입 예정
     */
    @Operation(summary = "주식 매수", description = "실시간 현재가 기준으로 매수를 처리합니다. X-Idempotency-Key 헤더 필수.")
    @PostMapping("/buy")
    public ResponseEntity<ApiRes<BuyRes>> buy(
            @Parameter(description = "중복 요청 방지용 UUID (버튼 클릭마다 새로 생성)", required = true)
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestParam Long userId, // TODO: Security 적용 후 제거
            @Valid @RequestBody BuyReq request
    ) {
        BuyRes response = tradeService.buy(userId, idempotencyKey, request);
        return ResponseEntity.ok(new ApiRes<>("매수가 완료되었습니다.", response));
    }

    //주식 매도
    @PostMapping("/sell")
    public ApiRes<String> sell(@RequestBody @Valid TradeSellReq req){
        tradeService.sell(req);

        return new ApiRes<String>(
                "매도 주문이 성공적으로 체결되었습니다.",
                "SUCCESS"
        );
    }
}
