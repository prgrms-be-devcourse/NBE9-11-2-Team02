package com.back.together02be.trade.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.together02be.asset.entity.UserAccount;
import com.back.together02be.asset.entity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.stock.dto.RealtimeStockPrice;
import com.back.together02be.stock.entity.Stock;
import com.back.together02be.stock.repository.StockRepository;
import com.back.together02be.stock.service.RealTimeStockPriceStore;
import com.back.together02be.trade.dto.BuyReq;
import com.back.together02be.trade.dto.BuyRes;
import com.back.together02be.trade.entity.Trade;
import com.back.together02be.trade.repository.TradeRepository;
import com.back.together02be.users.entity.Users;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TradeBuyProcessorTest {

    @Mock
    RealTimeStockPriceStore stockPriceStore;
    @Mock
    UserAccountRepository userAccountRepository;
    @Mock
    UserStockRepository userStockRepository;
    @Mock
    StockRepository stockRepository;
    @Mock
    TradeRepository tradeRepository;

    @InjectMocks
    TradeBuyProcessor tradeBuyProcessor;

    private Users user;
    private Stock stock;
    private UserAccount account;

    @BeforeEach
    void setUp() {
        user = new Users("testuser", "password", "테스트유저");
        account = new UserAccount(user, 0L, 50_000_000L);

        ReflectionTestUtils.setField(stock, "id", 1L);

        lenient().when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));
        // 잔고 차감 성공 기본값 (1 = 1행 업데이트됨)
        lenient().when(userAccountRepository.decreaseDepositIfSufficient(anyLong(), anyLong())).thenReturn(1);
        // 비관적 락 조회
        lenient().when(userAccountRepository.findByUsersIdWithLock(1L)).thenReturn(Optional.of(account));
        lenient().when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> {
            Trade trade = invocation.getArgument(0);
            ReflectionTestUtils.setField(trade, "id", 1L);
            return trade;
        });
    }

    private RealtimeStockPrice mockPrice(String stockCode, long price) {
        return RealtimeStockPrice.builder()
                .stockCode(stockCode)
                .price(String.valueOf(price))
                .build();
    }

    @Test
    @DisplayName("정상 매수 (신규 보유종목) — 잔고 차감 쿼리 호출, 거래 내역·UserStock 저장")
    void 정상_매수_신규_보유종목() {
        // given
        long price = 70_000L;
        long quantity = 10L;
        long amount = price * quantity;

        when(stockPriceStore.get("005930")).thenReturn(mockPrice("005930", price));
        when(userStockRepository.findByUsersIdAndStockId(1L, 1L)).thenReturn(Optional.empty());

        // when
        BuyRes response = tradeBuyProcessor.processBuy(1L, new BuyReq(1L, quantity));

        // then
        assertThat(response.price()).isEqualTo(price);
        assertThat(response.quantity()).isEqualTo(quantity);
        assertThat(response.amount()).isEqualTo(amount);

        // @Modifying으로 잔고 차감 쿼리가 호출됐는지 검증
        verify(userAccountRepository).decreaseDepositIfSufficient(1L, amount);
        verify(userStockRepository).save(any(UserStock.class));
        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    @DisplayName("추가 매수 — 평균매입가 재계산 검증")
    void 추가_매수_평균매입가_재계산() {
        // given
        long existingQty = 10L;
        long existingAvgPrice = 60_000L;
        long newPrice = 70_000L;
        long newQty = 10L;
        long expectedAvgPrice = (existingQty * existingAvgPrice + newQty * newPrice) / (existingQty + newQty);

        UserStock existing = new UserStock(user, stock, existingQty, existingAvgPrice);

        when(stockPriceStore.get("005930")).thenReturn(mockPrice("005930", newPrice));
        when(userStockRepository.findByUsersIdAndStockId(1L, 1L)).thenReturn(Optional.of(existing));

        // when
        tradeBuyProcessor.processBuy(1L, new BuyReq(1L, newQty));

        // then
        assertThat(existing.getQuantity()).isEqualTo(existingQty + newQty);
        assertThat(existing.getAveragePrice()).isEqualTo(expectedAvgPrice);

        verify(userStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("잔고 부족 — @Modifying이 0 반환 시 예외 발생")
    void 잔고_부족_예외() {
        // given
        long price = 70_000L;
        long quantity = 1_000L;
        long amount = price * quantity; // 7,000만원 > 잔고 5,000만원

        when(stockPriceStore.get("005930")).thenReturn(mockPrice("005930", price));
        when(userAccountRepository.decreaseDepositIfSufficient(1L, amount)).thenReturn(0); // 잔고 부족
        when(userAccountRepository.findByUsersId(1L)).thenReturn(Optional.of(account));   // 에러 메시지용

        // when & then
        assertThatThrownBy(() -> tradeBuyProcessor.processBuy(1L, new BuyReq(1L, quantity)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔고가 부족합니다");
    }

    @Test
    @DisplayName("현재가 없음 — 예외 발생")
    void 현재가_없을때_예외() {
        // given: 스토어에 현재가 없음 (WebSocket 미수신 상태)
        when(stockPriceStore.get("005930")).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> tradeBuyProcessor.processBuy(1L, new BuyReq(1L, 10L)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("현재가 정보");
    }
}
