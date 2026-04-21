package com.back.together02be.trade.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.back.together02be.asset.entity.UserAccount;
import com.back.together02be.asset.entity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.global.idempotency.IdempotencyKey;
import com.back.together02be.global.idempotency.IdempotencyKeyRepository;
import com.back.together02be.stock.dto.RealtimeStockPrice;
import com.back.together02be.stock.entity.Stock;
import com.back.together02be.stock.entity.StockMarket;
import com.back.together02be.stock.repository.StockRepository;
import com.back.together02be.stock.service.RealTimeStockPriceStore;
import com.back.together02be.trade.dto.BuyReq;
import com.back.together02be.trade.repository.TradeRepository;
import com.back.together02be.users.entity.Users;
import com.back.together02be.users.repository.UsersRepository;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 동시성 통합 테스트.
 *
 * @Transactional 미사용 — 동시 스레드가 커밋된 데이터를 읽어야 하므로
 * 각 테스트 후 @AfterEach에서 데이터를 직접 정리한다.
 */
@SpringBootTest
class TradeConcurrencyTest {

    @Autowired
    TradeBuyProcessor tradeBuyProcessor;

    @Autowired
    UserAccountRepository userAccountRepository;

    @Autowired
    UserStockRepository userStockRepository;

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    StockRepository stockRepository;

    @Autowired
    TradeRepository tradeRepository;

    @Autowired
    IdempotencyKeyRepository idempotencyKeyRepository;

    @MockitoBean
    RealTimeStockPriceStore stockPriceStore;

    private Users user;
    private Stock stock;

    @BeforeEach
    void setUp() {
        user = usersRepository.save(new Users("concurrency_user", "pw", "동시성테스트유저"));
        stock = stockRepository.save(new Stock("999999", "테스트종목", StockMarket.KOSPI));

        when(stockPriceStore.get("999999")).thenReturn(
                RealtimeStockPrice.builder()
                        .stockCode("999999")
                        .price("70000")
                        .build()
        );
    }

    @AfterEach
    void tearDown() {
        // 외래키 참조 순서대로 삭제: Trade → UserStock → UserAccount → IdempotencyKey → Stock → Users
        tradeRepository.deleteAll(
                tradeRepository.findAll().stream()
                        .filter(t -> t.getStock().getId().equals(stock.getId()))
                        .toList()
        );
        userStockRepository.findByUsersIdAndStockId(user.getId(), stock.getId())
                .ifPresent(userStockRepository::delete);
        userAccountRepository.findByUsersId(user.getId())
                .ifPresent(userAccountRepository::delete);
        idempotencyKeyRepository.deleteAll(
                idempotencyKeyRepository.findAll().stream()
                        .filter(k -> k.getUserId().equals(user.getId()))
                        .toList()
        );
        stockRepository.delete(stock);
        usersRepository.delete(user);
    }

    @Test
    @DisplayName("잔고 부족 — 동시 요청 중 하나만 성공")
    void 잔고_부족_동시_요청_하나만_성공() throws InterruptedException {
        // given: 잔고 100만원, 각 요청 70만원 → 한 건만 통과 가능
        long deposit = 1_000_000L;
        long price = 70_000L;
        long quantity = 10L; // 70만원
        userAccountRepository.save(new UserAccount(user, 0L, deposit));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    String key = UUID.randomUUID().toString();
                    idempotencyKeyRepository.save(new IdempotencyKey(key, user.getId()));
                    startLatch.await();
                    tradeBuyProcessor.processBuy(user.getId(), key, new BuyReq(stock.getId(), quantity, 70_000L));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        UserAccount result = userAccountRepository.findByUsersId(user.getId()).orElseThrow();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
        assertThat(result.getDeposit()).isEqualTo(deposit - price * quantity); // 30만원
        assertThat(result.getDeposit()).isGreaterThanOrEqualTo(0); // 음수 잔고 없음

        System.out.printf("[잔고 부족 동시 요청] 성공: %d, 실패: %d, 잔고: %,d원%n",
                successCount.get(), failCount.get(), result.getDeposit());
    }

    @Test
    @DisplayName("잔고 충분 — 동시 첫 매수 둘 다 성공, UserStock 수량 정확")
    void 잔고_충분_동시_첫_매수_둘_다_성공() throws InterruptedException {
        // given: 잔고 200만원, 각 요청 70만원 → 둘 다 통과
        long deposit = 2_000_000L;
        long quantity = 10L; // 각 70만원
        userAccountRepository.save(new UserAccount(user, 0L, deposit));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    String key = UUID.randomUUID().toString();
                    idempotencyKeyRepository.save(new IdempotencyKey(key, user.getId()));
                    startLatch.await();
                    tradeBuyProcessor.processBuy(user.getId(), key, new BuyReq(stock.getId(), quantity, 70_000L));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("[실패] " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        UserStock userStock = userStockRepository
                .findByUsersIdAndStockId(user.getId(), stock.getId())
                .orElseThrow();
        UserAccount result = userAccountRepository.findByUsersId(user.getId()).orElseThrow();

        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(userStock.getQuantity()).isEqualTo(quantity * 2); // 20주
        assertThat(result.getDeposit()).isEqualTo(deposit - 70_000L * quantity * 2); // 60만원

        System.out.printf("[잔고 충분 동시 요청] 성공: %d, 수량: %d주, 잔고: %,d원%n",
                successCount.get(), userStock.getQuantity(), result.getDeposit());
    }
}
