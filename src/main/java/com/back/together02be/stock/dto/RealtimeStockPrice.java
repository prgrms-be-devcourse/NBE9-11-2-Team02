package com.back.together02be.stock.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Getter
@Builder
public class RealtimeStockPrice {
	private String stockCode;      // 종목코드
	private String price;          // 현재가
	private String changeSign;     // 전일 대비 부호 (1:상한 2:상승 3:보합 4:하한 5:하락)
	private String change;         // 전일 대비
	private String changeRate;     // 전일 대비율
	private String tradeTime;	   // 체결시간

	// 10초 이상 지연시 예외처리
	public boolean isStale(int limitSeconds) {
		if (this.tradeTime == null) return true; // 데이터가 없으면 무조건 지연 간주

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmss");
		LocalTime tradeTime = LocalTime.parse(this.tradeTime, formatter);

		LocalDateTime tradeDateTime = LocalDateTime.of(LocalDate.now(), tradeTime);
		LocalDateTime now = LocalDateTime.now();

		// 현재 시간과 체결 시간의 차이가 limitSeconds를 넘는지 확인
		return ChronoUnit.SECONDS.between(tradeDateTime, now) > limitSeconds;
	}
}
