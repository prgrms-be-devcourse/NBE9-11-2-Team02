package com.back.together02be.infra.kis;

import org.java_websocket.WebSocket;
import org.springframework.stereotype.Component;

import com.back.together02be.infra.kis.constant.KisConstants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KisWebSocketHandler {

	private String approvalKey;

	public void setApprovalKey(String approvalKey) {
		this.approvalKey = approvalKey;
	}

	public void onOpen(WebSocket conn) {
		// 삼성전자 구독 (테스트용)
		subscribe(conn, "005930");
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

	public void onMessage(String message) {
		if (message.startsWith("{")) {
			log.info("제어 메시지: {}", message);
			return;
		}
		parse(message);
	}

	private void parse(String raw) {
		String[] parts = raw.split("\\|");
		if (parts.length < 4)
			return;

		String[] fields = parts[3].split("\\^");

		String stockCode = fields[KisConstants.FIELD_STOCK_CODE]; // 종목 코드
		String price = fields[KisConstants.FIELD_CURRENT_PRICE]; // 주식 현재가
		String changeSign = fields[KisConstants.FIELD_CHANGE_SIGN]; // 전일 대비 부호
		String priceChange = fields[KisConstants.FIELD_CHANGE]; // 전일 대비
		String priceChangeRate = fields[KisConstants.FIELD_CHANGE_RATE]; // 전일 대비율

		log.info("[{}] 현재가: {}원 | 전일 대비 부호: {} | 전일 대비 가격: {} | 전일 대비율: {}",
			stockCode, price, changeSign, priceChange, priceChangeRate);
	}
}
