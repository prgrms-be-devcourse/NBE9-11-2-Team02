package com.back.together02be.asset.controller;


import com.back.together02be.asset.enitity.UserAccount;
import com.back.together02be.asset.service.AssetService;
import com.back.together02be.global.apiRes.ApiRes;
import com.back.together02be.users.enitity.Users;
import com.back.together02be.users.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.hibernate.service.spi.ServiceException;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Member;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/asset")
@Tag(name = "AssetController", description = "자산 API")
public class AssetController {
    private final AssetService assetService;
    private final UsersService usersService;

    record TotalPurchaseResBody(
            long totalAmount
    ) { }

    @GetMapping("accounts/{userId}")
    @Operation(summary = "총 매수금 조회")
    public ApiRes<TotalPurchaseResBody> totalPrice(@PathVariable long userId){
        //인증된 사용자 정보 가져오기
        //Users actor= rq.getActor();
        //userId = 1L;
        //총매수액 가져오기
        long totalPurchase = assetService.getTotalAmountByUserId(userId);

        return new ApiRes<>(
                "조회가 완료되었습니다.",
                new TotalPurchaseResBody(totalPurchase)
        );
    }
}
