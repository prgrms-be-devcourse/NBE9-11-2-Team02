package com.back.together02be.trade.controller;

import com.back.together02be.global.apiRes.ApiRes;
import com.back.together02be.global.security.SecurityUser;
import com.back.together02be.trade.dto.BuyReq;
import com.back.together02be.trade.dto.BuyRes;
import com.back.together02be.trade.dto.request.TradeSellReq;
import com.back.together02be.trade.dto.response.TradeSellRes;
import com.back.together02be.trade.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trades")
@Tag(name = "TradeController", description = "주식 거래 API")
public class TradeController {

    private final TradeService tradeService;

    @Operation(summary = "주식 매수", description = "실시간 현재가 기준으로 매수를 처리합니다. X-Idempotency-Key 헤더 필수.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매수 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류 또는 잔고 부족"),
            @ApiResponse(responseCode = "404", description = "주식 정보 없음 / 계좌 없음 / 현재가 없음"),
            @ApiResponse(responseCode = "409", description = "중복 요청 (이미 처리된 요청)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/buy")
    public ResponseEntity<ApiRes<BuyRes>> buy(
            @AuthenticationPrincipal SecurityUser user,
            @Parameter(description = "중복 요청 방지용 UUID (버튼 클릭마다 새로 생성)", required = true)
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody BuyReq request
    ) {
        BuyRes response = tradeService.buy(user.getId(), idempotencyKey, request);
        return ResponseEntity.ok(new ApiRes<>("매수가 완료되었습니다.", response));
    }

    //주식 매도
    @Operation(summary = "주식 매도", description = "실시간 현재가 기준으로 매도를 처리합니다. X-Idempotency-Key 헤더 필수.")
    @PostMapping("/sell")
    public ResponseEntity<ApiRes<TradeSellRes>> sell(
            @Parameter(description = "중복 요청 방지용 UUID (버튼 클릭마다 새로 생성)", required = true)
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestParam Long userId, // TODO: Security 적용 후 제거
            @RequestBody @Valid TradeSellReq req
    ){
        TradeSellRes res = tradeService.sell(userId,idempotencyKey,req);
        return ResponseEntity.ok(new ApiRes<>("매수가 완료되었습니다.", res));
    }
}
