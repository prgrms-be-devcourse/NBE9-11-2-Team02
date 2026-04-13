package com.back.together02be.infra.kis.constant;

public class KisConstants {
	public static final String TR_REALTIME_PRICE = "H0STCNT0"; // 실시간 체결가

	// H0STCNT0 수신 필드 인덱스('^' 구분)
	public static final int FIELD_CURRENT_PRICE      = 2;  // STCK_PRPR, 주식 현재가
	public static final int FIELD_CHANGE_SIGN        = 3;  // PRDY_VRSS_SIGN, 전일 대비 부호
	public static final int FIELD_CHANGE             = 4;  // PRDY_VRSS, 전일 대비
	public static final int FIELD_CHANGE_RATE        = 5;  // PRDY_CTRT, 전일 대비율

}
