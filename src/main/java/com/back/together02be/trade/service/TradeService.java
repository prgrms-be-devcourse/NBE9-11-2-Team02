package com.back.together02be.trade.service;

import com.back.together02be.asset.entity.UserAccount;
import com.back.together02be.asset.entity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.global.idempotency.IdempotencyService;
import com.back.together02be.stock.dto.RealtimeStockPrice;
import com.back.together02be.stock.entity.Stock;
import com.back.together02be.stock.repository.StockRepository;
import com.back.together02be.stock.service.RealTimeStockPriceStore;
import com.back.together02be.trade.dto.BuyReq;
import com.back.together02be.trade.dto.BuyRes;
import com.back.together02be.trade.dto.request.TradeSellReq;
import com.back.together02be.trade.entity.Trade;
import com.back.together02be.trade.processor.TradeBuyProcessor;
import com.back.together02be.trade.repository.TradeRepository;
import com.back.together02be.users.entity.Users;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TradeRepository tradeRepository;
    private final UserStockRepository userStockRepository;
    private final UserAccountRepository userAccountRepository;
    private final StockRepository stockRepository;
    private final RealTimeStockPriceStore realtimeStockPriceService;

    @Transactional
    public void sell(@Valid TradeSellReq req){
        // 1. 보유 주식 조회 + X-Lock
        //    (users_id, stock_id) 단위 락 → 다른 종목은 병렬 처리
        UserStock userStock = userStockRepository.findByUsersIdAndStockIdWithLock(req.userId(),req.stockId())
                .orElseThrow(() -> new IllegalArgumentException("보유하지 않은 주식입니다."));

        //2. 현재가 가져오기
        RealtimeStockPrice stockPrice = realtimeStockPriceService.get(userStock.getStock().getStockCode());
        if (stockPrice == null) {
            throw new EntityNotFoundException("현재가 정보를 불러올 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
        Long price = Long.parseLong(stockPrice.getPrice());

        //3. 수량 검증
        if(userStock.getQuantity()<req.quantity()){
            throw new IllegalArgumentException("보유 수량이 부족합니다.");
        }
        // 4. Trade에 쓸 참조 미리 꺼내기 (삭제 전에!)
        Users users = userStock.getUsers();
        Stock stock = userStock.getStock();

        // 5. 수익 / 금액 계산
        long profit = (price - userStock.getAveragePrice()) * req.quantity();
        long amount = price * req.quantity();

        // 6. 계좌 조회 + X-Lock → 예수금 증가
        UserAccount userAccount = userAccountRepository
                .findByUsersIdWithLock(req.userId())
                .orElseThrow(() -> new IllegalArgumentException("계좌가 존재하지 않습니다."));

        userAccount.addDeposit(amount);
        userAccount.subtractTotalPurchase(userStock.getAveragePrice() * req.quantity());

        // 7. 수량 차감 or 전량 매도 시 삭제 (한 곳에서만!)
        if (userStock.getQuantity().equals(req.quantity())) {
            userStockRepository.delete(userStock);  // 전량 매도
        } else {
            userStock.updateQuantity(userStock.getQuantity() - req.quantity());  // 부분 매도
        }

        // 8. 거래 내역 저장
        Trade trade = Trade.sell(users, stock, req.quantity(),price, profit);
        tradeRepository.save(trade);
    }
}
