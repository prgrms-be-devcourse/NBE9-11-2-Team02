package com.back.together02be.asset.controller;


import com.back.together02be.asset.dto.response.StockInfoRes;
import com.back.together02be.asset.dto.response.TotalPurchaseRes;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.asset.service.AssetService;
import com.back.together02be.global.apiRes.ApiRes;
import com.back.together02be.users.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/asset")
@Tag(name = "AssetController", description = "자산 API")
public class AssetController {
    private final AssetService assetService;
    private final UsersService usersService;


    @GetMapping("/accounts/{userId}")
    @Operation(summary = "총 매수금 조회")
    public ApiRes<TotalPurchaseRes> totalPrice(@PathVariable long userId){
        //인증된 사용자 정보 가져오기
        //Users actor= rq.getActor();
        //userId = 1L;
        //총매수액 가져오기
        long totalPurchase = assetService.getTotalAmountByUserId(userId);
        List<StockInfoRes> stockInfos = assetService.getStockInfo(userId);

        return new ApiRes<>(
                "조회가 완료되었습니다.",
                new TotalPurchaseRes(totalPurchase,stockInfos)
        );
    }
}
