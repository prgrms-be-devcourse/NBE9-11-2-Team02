package com.back.together02be.trade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.together02be.global.exception.DuplicateRequestException;
import com.back.together02be.global.idempotency.IdempotencyService;
import com.back.together02be.trade.dto.BuyReq;
import com.back.together02be.trade.dto.BuyRes;
import com.back.together02be.trade.processor.TradeBuyProcessor;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    TradeBuyProcessor tradeBuyProcessor;

    @Mock
    IdempotencyService idempotencyService;

    @InjectMocks
    TradeService tradeService;

    @Test
    @DisplayName("1000명 다른 유저 동시 매수 — 모두 성공, 오류 없음")
    void 천명_다른_유저_동시_매수() throws InterruptedException {
        // given
        int userCount = 1000;
        BuyReq request = new BuyReq(1L, 10L);
        BuyRes mockResponse = new BuyRes(1L, "삼성전자", 10L, 70_000L, 700_000L, 49_300_000L);

        when(idempotencyService.registerIfAbsent(anyString(), anyLong())).thenReturn(true);
        when(tradeBuyProcessor.processBuy(anyLong(), any())).thenReturn(mockResponse);

        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        CountDownLatch startLatch = new CountDownLatch(1); // 전 스레드 동시 출발 신호
        CountDownLatch doneLatch = new CountDownLatch(userCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (long userId = 1; userId <= userCount; userId++) {
            final long id = userId;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 준비될 때까지 대기
                    tradeService.buy(id, UUID.randomUUID().toString(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        long start = System.currentTimeMillis();
        startLatch.countDown();                      // 동시 출발
        doneLatch.await(10, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(userCount);
        assertThat(failCount.get()).isEqualTo(0);
        verify(tradeBuyProcessor, times(userCount)).processBuy(anyLong(), any());

        System.out.printf("[결과] 1000명 처리 완료: %dms%n", elapsed);
    }

    @Test
    @DisplayName("같은 멱등성 키 중복 요청 — 두 번째 요청 차단")
    void 같은_키_중복_요청_차단() {
        // given
        String idempotencyKey = UUID.randomUUID().toString();
        BuyReq request = new BuyReq(1L, 10L);
        BuyRes mockResponse = new BuyRes(1L, "삼성전자", 10L, 70_000L, 700_000L, 49_300_000L);

        when(idempotencyService.registerIfAbsent(idempotencyKey, 1L))
                .thenReturn(true)    // 첫 번째 요청: 통과
                .thenReturn(false);  // 두 번째 요청: 차단
        when(tradeBuyProcessor.processBuy(anyLong(), any())).thenReturn(mockResponse);

        // when & then
        assertThatNoException().isThrownBy(() -> tradeService.buy(1L, idempotencyKey, request));

        assertThatThrownBy(() -> tradeService.buy(1L, idempotencyKey, request))
                .isInstanceOf(DuplicateRequestException.class)
                .hasMessage("이미 처리된 요청입니다.");
    }

    @Test
    @DisplayName("처리 실패 시 멱등성 키 반납 — 동일 키로 재시도 허용")
    void 실패_시_키_반납_재시도_가능() {
        // given
        String idempotencyKey = UUID.randomUUID().toString();
        BuyReq request = new BuyReq(1L, 10L);
        BuyRes mockResponse = new BuyRes(1L, "삼성전자", 10L, 70_000L, 700_000L, 49_300_000L);

        when(idempotencyService.registerIfAbsent(idempotencyKey, 1L)).thenReturn(true);
        when(tradeBuyProcessor.processBuy(anyLong(), any()))
                .thenThrow(new RuntimeException("일시적 오류"))  // 첫 번째: 실패
                .thenReturn(mockResponse);                        // 두 번째: 성공

        // when: 첫 번째 요청 실패
        assertThatThrownBy(() -> tradeService.buy(1L, idempotencyKey, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("일시적 오류");

        // then: 실패 시 키 반납 확인
        verify(idempotencyService).remove(idempotencyKey);

        // when & then: 동일 키로 재시도 성공
        BuyRes result = tradeService.buy(1L, idempotencyKey, request);
        assertThat(result).isNotNull();
    }
}
