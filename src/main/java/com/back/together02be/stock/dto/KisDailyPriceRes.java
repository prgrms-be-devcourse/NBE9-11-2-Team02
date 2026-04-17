//주식 현재가 일자별 API
package com.back.together02be.stock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisDailyPriceRes(
        Output output
) {
    public record Output(

            //종가
            @JsonProperty("stck_clpr")
            String closePrice
    ) {}
}
