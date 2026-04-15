package com.back.together02be.stock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisTokenRes(

        // API 인증 토큰 (Bearer 토큰)
        @JsonProperty("access_token")
        String accessToken,

        // 토큰 타입 (보통 "Bearer")
        @JsonProperty("token_type")
        String tokenType,

        // 토큰 유효 시간 (초 단위)
        @JsonProperty("expires_in")
        Integer expiresIn
) {
}
