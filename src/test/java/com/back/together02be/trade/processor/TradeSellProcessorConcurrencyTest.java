package com.back.together02be.trade.processor;

import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.trade.util.MarketTimeValidator;
import com.back.together02be.trade.dto.request.TradeSellReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mockStatic;
@SpringBootTest
class TradeSellProcessorConcurrencyTest {

    // [핵심] 이 부분이 클래스 멤버 변수로 선언되어 있어야 합니다!
    @Autowired
    private TradeSellProcessor tradeSellProcessor;

    // [추가] 데이터 초기화를 위해 필요할 수 있습니다
    @Autowired
    private UserStockRepository userStockRepository;

    @Test
    @DisplayName("동시 매도 요청 시 데이터 무결성 검증")
    void concurrencyTest() throws InterruptedException {
        // 1. static 메서드 모킹 (테스트 동안만 적용됨)
        try (MockedStatic<MarketTimeValidator> marketValidator = mockStatic(MarketTimeValidator.class)) {
            // validateMarketOpen 호출 시 아무것도 하지 않도록 설정
            marketValidator.when(MarketTimeValidator::validateMarketOpen).thenAnswer(invocation -> null);

            // 2. 초기 세팅: 100주 보유 (테스트 데이터 세팅)
            Long userId = 1L;
            Long stockId = 1L;

            int threadCount = 10;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // 3. 10개의 스레드가 동시에 매도 시도
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        tradeSellProcessor.processSell(userId, new TradeSellReq(userId, stockId, 20L, 50000L));
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();

            // 4. 검증
            assertThat(successCount.get()).isEqualTo(5);
            assertThat(failCount.get()).isEqualTo(5);
        }
    }
}