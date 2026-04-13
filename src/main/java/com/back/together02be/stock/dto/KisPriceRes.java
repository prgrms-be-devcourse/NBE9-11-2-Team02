package com.back.together02be.stock.dto;

//한투 현재가 조회 API의 전체 응답을 받는 객체
public record KisPriceRes(
        String rt_cd,
        String msg_cd,
        String msg1,
        Output output
) {
    // 우리가 실제 쓰고 싶은 핵심 데이터, 필요한 정보들
    public record Output(
            String stck_prpr,
            String prdy_vrss,
            String prdy_ctrt
    ) {
    }
}