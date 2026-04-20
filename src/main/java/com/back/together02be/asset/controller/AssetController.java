package com.back.together02be.asset.controller;

import com.back.together02be.asset.dto.response.UserStockRes;
import com.back.together02be.asset.service.AssetService;
import com.back.together02be.global.apiRes.ApiRes;
import com.back.together02be.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/asset")
@Tag(name = "AssetController", description = "자산 API")
public class AssetController {

    private final AssetService assetService;

    @GetMapping("/stocks")
    @Operation(summary = "보유 종목 조회")
    public ResponseEntity<ApiRes<List<UserStockRes>>> getUserStocks(@AuthenticationPrincipal SecurityUser user) {
        List<UserStockRes> userStocks = assetService.getUserStocks(user.getId());
        return ResponseEntity.ok(new ApiRes<>("보유 종목 조회 성공", userStocks));
    }
}
