package com.back.together02be.stock.dto;

public record KisTokenRes(
        String access_token, // API 호출 시, 인증된 사용자라고 증명하는 값
        String token_type, // access_token을 어떻게 쓰는지 알려주는 타입(Bearer)
        Integer expires_in // 토큰 유효 시간(초 단위)
) {
}
