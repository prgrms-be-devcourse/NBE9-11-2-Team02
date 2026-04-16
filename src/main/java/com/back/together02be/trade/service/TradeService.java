package com.back.together02be.trade.service;

import com.back.together02be.global.idempotency.IdempotencyService;
import com.back.together02be.trade.dto.BuyReq;
import com.back.together02be.trade.dto.BuyRes;
import com.back.together02be.trade.processor.TradeBuyProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeBuyProcessor tradeBuyProcessor;
    private final IdempotencyService idempotencyService;

    /**
     * TR-01 매수
     * <p>
     * 멱등성 키 — DB UNIQUE 제약으로 중복 전송 차단 서버 재시작 후에도 DB에 키가 남아있어 중복 체결 방지
     *
     * @Transactional 잔고 검증 — 이중 차감 원천 차단
     */
    public BuyRes buy(Long userId, String idempotencyKey, BuyReq request) {
        if (!idempotencyService.registerIfAbsent(idempotencyKey, userId)) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        try {
            return tradeBuyProcessor.processBuy(userId, request);
        } catch (Exception e) {
            idempotencyService.remove(idempotencyKey); // 실패 시 키 반납 → 재시도 허용
            throw e;
        }
    }
}
