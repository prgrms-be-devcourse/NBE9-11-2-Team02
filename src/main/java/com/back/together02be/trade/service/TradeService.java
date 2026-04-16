package com.back.together02be.trade.service;

import com.back.together02be.asset.enitity.UserAccount;
import com.back.together02be.asset.enitity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.stock.enitity.RealtimeStockPrice;
import com.back.together02be.stock.enitity.Stock;
import com.back.together02be.stock.repository.StockRepository;
import com.back.together02be.stock.service.RealtimeStockPriceService;
import com.back.together02be.trade.controller.TradeController;
import com.back.together02be.trade.enitity.Trade;
import com.back.together02be.trade.enitity.TradeType;
import com.back.together02be.trade.repository.TradeRepository;
import com.back.together02be.users.entity.Users;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TradeService {
    private final TradeRepository tradeRepository;
    private final UserStockRepository userStockRepository;
    private final UserAccountRepository userAccountRepository;
    private final StockRepository stockRepository;
    private final RealtimeStockPriceService realtimeStockPriceService;

    public void sell(TradeController.TradeSellReq req){
        // 1. 보유 주식 조회 + X-Lock
        //    (users_id, stock_id) 단위 락 → 다른 종목은 병렬 처리
        UserStock userStock = userStockRepository.findByUsersIdAndStockIdWithLock(req.userId(),req.stockId())
                .orElseThrow(() -> new IllegalArgumentException("보유하지 않은 주식입니다."));
        //2. 현재가 가져오기
        RealtimeStockPrice stockPrice = realtimeStockPriceService.get(userStock.getStock().getStockCode());
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
