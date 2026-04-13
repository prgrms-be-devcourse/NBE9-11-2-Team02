package com.back.together02be.asset.dto.response;

import com.back.together02be.asset.controller.AssetController;

import java.util.List;

public record TotalPurchaseRes(
        long totalAmount,
        List<StockInfoRes> stocks
) { }