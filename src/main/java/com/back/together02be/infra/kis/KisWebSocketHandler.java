package com.back.together02be.infra.kis;

import org.java_websocket.WebSocket;
import org.springframework.stereotype.Component;

import com.back.together02be.infra.kis.constant.KisConstants;
import com.back.together02be.stock.dto.RealtimeStockPrice;
import com.back.together02be.stock.service.RealTimeStockPriceStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisWebSocketHandler {

	private final RealTimeStockPriceStore rtStockPriceStore;

	private String approvalKey;

	public void setApprovalKey(String approvalKey) {
		this.approvalKey = approvalKey;
	}

	public void onOpen(WebSocket conn) {
		log.info("한국투자 증권 WebSocket 연결 성공");
	}

	public void subscribe(WebSocket conn, String stockCode) {
		String message = """
			{
			  "header": {
			    "approval_key": "%s",
			    "custtype": "P",
			    "tr_type": "1",
			    "content-type": "utf-8"
			  },
			  "body": {
			    "input": {
			      "tr_id": "%s",
			      "tr_key": "%s"
			    }
			  }
			}
			""".formatted(approvalKey, KisConstants.TR_REALTIME_PRICE, stockCode);

		conn.send(message);
		log.info("구독 시작: {}", stockCode);
	}

	public void unsubscribe(WebSocket conn, String stockCode) {
		String message = """
        {
          "header": {
            "approval_key": "%s",
            "custtype": "P",
            "tr_type": "2",
            "content-type": "utf-8"
          },
          "body": {
            "input": {
              "tr_id": "%s",
              "tr_key": "%s"
            }
          }
        }
        """.formatted(approvalKey, KisConstants.TR_REALTIME_PRICE, stockCode);

		conn.send(message);
		log.info("구독 취소: {}", stockCode);
	}

	public void onMessage(String message) {
		if (message.startsWith("{")) {
			log.info("제어 메시지: {}", message);
			return;
		}
		parse(message);
	}

	private void parse(String raw) {
		// 포맷: 0|H0STCNT0|004|005930^150000^70800^...
		String[] parts = raw.split("\\|");
		if (parts.length < 4)
			return;

		String[] fields = parts[3].split("\\^");

		RealtimeStockPrice stockPrice = RealtimeStockPrice.builder()
			.stockCode(fields[KisConstants.FIELD_STOCK_CODE]) // 종목 코드
			.tradeTime(fields[KisConstants.FIELD_TRADE_TIME]) // 체결 시간
			.price(fields[KisConstants.FIELD_CURRENT_PRICE]) // 주식 현재가
			.changeSign(fields[KisConstants.FIELD_CHANGE_SIGN]) // 전일 대비 부호
			.change(fields[KisConstants.FIELD_CHANGE]) // 전일 대비
			.changeRate(fields[KisConstants.FIELD_CHANGE_RATE]) // 전일 대비율
			.build();

		rtStockPriceStore.put(stockPrice.getStockCode(), stockPrice); // ← 캐싱
		log.info("[{}] 체결시간: {}, 현재가: {}원 | 전일 대비 부호: {} | 전일 대비 가격: {} | 전일 대비율: {}",
			stockPrice.getStockCode(), stockPrice.getTradeTime(), stockPrice.getPrice(), stockPrice.getChangeSign(),
			stockPrice.getChange(), stockPrice.getChangeRate());
	}
}
