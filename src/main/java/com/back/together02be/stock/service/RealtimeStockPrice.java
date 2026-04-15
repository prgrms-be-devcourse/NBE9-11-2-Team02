package com.back.together02be.stock.service;

import lombok.Builder;
import lombok.Getter;

// TODO: 팀장 코드 올라오면 이 파일 삭제하고 팀장 파일로 교체
@Getter
@Builder
public class RealtimeStockPrice {
	private String stockCode;   // 종목코드
	private String price;       // 현재가
	private String changeSign;  // 전일 대비 부호 (1:상한 2:상승 3:보합 4:하한 5:하락)
	private String change;      // 전일 대비
	private String changeRate;  // 전일 대비율
	private String tradeTime;   // 체결시간
}
