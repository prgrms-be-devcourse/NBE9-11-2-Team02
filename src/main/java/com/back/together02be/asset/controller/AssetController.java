package com.back.together02be.asset.controller;

import com.back.together02be.asset.dto.response.UserStockRes;
import com.back.together02be.asset.service.AssetService;
import com.back.together02be.global.apiRes.ApiRes;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/asset")
@Tag(name = "AssetController", description = "자산 API")
public class AssetController {

    private final AssetService assetService;

    @GetMapping("/stocks/{userId}")
    public ResponseEntity<ApiRes<List<UserStockRes>>> getUserStocks(@PathVariable Long userId) {
        List<UserStockRes> userStocks = assetService.getUserStocks(userId);
        return ResponseEntity.ok(new ApiRes<>("보유 종목 조회 성공", userStocks));
    }
}
