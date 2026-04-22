package com.back.together02be.trade.processor;

import com.back.together02be.asset.entity.UserAccount;
import com.back.together02be.asset.entity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.trade.util.MarketTimeValidator;
import com.back.together02be.stock.dto.RealtimeStockPrice;
import com.back.together02be.stock.entity.Stock;
import com.back.together02be.stock.repository.StockRepository;
import com.back.together02be.stock.service.RealTimeStockPriceStore;
import com.back.together02be.trade.dto.request.TradeSellReq;
import com.back.together02be.trade.dto.response.TradeSellRes;
import com.back.together02be.trade.entity.Trade;
import com.back.together02be.trade.repository.TradeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class TradeSellProcessor {
    private final RealTimeStockPriceStore stockPriceStore;
    private final UserAccountRepository userAccountRepository;
    private final UserStockRepository userStockRepository;
    private final StockRepository stockRepository;
    private final TradeRepository tradeRepository;
    private static final BigDecimal SELL_TOLERANCE_RATE = new BigDecimal("0.98");

    // 10초 이상 지연시 예외처리
    // 비즈니스 로직을 처리하는 프로세서 내부로 이동
    private boolean isStale(RealtimeStockPrice stockPrice, int limitSeconds) {
        String tradeTimeStr = stockPrice.getTradeTime(); // DTO에서 값을 가져옴
        if (tradeTimeStr == null) return true;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmss");
        LocalTime tradeTime = LocalTime.parse(tradeTimeStr, formatter);

        // 현재 날짜와 매칭
        LocalDateTime tradeDateTime = LocalDateTime.of(LocalDate.now(), tradeTime);
        LocalDateTime now = LocalDateTime.now();

        return ChronoUnit.SECONDS.between(tradeDateTime, now) > limitSeconds;
    }

    @Transactional
    public TradeSellRes processSell(Long userId, TradeSellReq request) {
        //0.장 마감 조회
        MarketTimeValidator.validateMarketOpen();

        // 1. 주식 정보 조회 및 보유 주식 조회
        Stock stock = stockRepository.findById(request.stockId())
                .orElseThrow(() -> new EntityNotFoundException("주식 정보가 없습니다."));

        UserStock userStock = userStockRepository.findByUsersIdAndStockId(request.userId(),request.stockId())
                .orElseThrow(()->new EntityNotFoundException("보유하지 않은 주식입니다."));

        UserAccount account = userAccountRepository.findByUsersId(userId)
                .orElseThrow(() -> new EntityNotFoundException("계좌 정보가 없습니다."));

        // 2. 현재가 조회 — KIS WebSocket 수신 후 RealTimeStockPriceStore에 저장된 실시간 가격
        RealtimeStockPrice stockPrice = stockPriceStore.get(stock.getStockCode());
        if (stockPrice == null) {
            throw new EntityNotFoundException("현재가 정보를 불러올 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
        if (isStale(stockPrice, 10)) {
            throw new IllegalStateException("시세 정보가 10초 이상 지연되었습니다. 거래가 불가능합니다.");
        }
        Long price = Long.parseLong(stockPrice.getPrice());

        // 슬리피지 검증 0.98을 BigDecimal로 표현
        BigDecimal minPrice = BigDecimal.valueOf(request.expectedPrice())
                .multiply(SELL_TOLERANCE_RATE)
                .setScale(0, RoundingMode.FLOOR);

        if (BigDecimal.valueOf(price).compareTo(minPrice) < 0) {
            throw new IllegalStateException("가격 변동폭이 커서 매도 주문이 거부되었습니다.");
        }

        // 3. 수량 검증
        int updatedRows = userStockRepository.updateQuantity(userId, request.stockId(), request.quantity());

        if (updatedRows == 0) {
            throw new IllegalStateException("보유 수량이 부족합니다.");
        }

        // 5. 수익 / 금액 계산
        long profit = (price - userStock.getAveragePrice()) * request.quantity();
        long amount = price * request.quantity();

        //6. 예수금 증가
        account.addDeposit(amount);
        account.subtractTotalPurchase(userStock.getAveragePrice()* request.quantity());

        //7. 수량 차감 및 전량 매도시 삭제
        if(userStock.getQuantity().equals(request.quantity())) {
            userStockRepository.deleteByUserAndStock(userId, request.stockId());
        }

        //8. 거래 내역 저장
        Trade trade = Trade.sell(account.getUsers(), stock, request.quantity(), price,profit);
        tradeRepository.save(trade);

        return new TradeSellRes(
                trade.getId(),
                stock.getStockName(),
                request.quantity(),
                price,
                amount,
                account.getDeposit()
        );
    }
}
