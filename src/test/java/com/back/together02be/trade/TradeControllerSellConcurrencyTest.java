package com.back.together02be.trade;

import com.back.together02be.asset.enitity.UserAccount;
import com.back.together02be.asset.enitity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.stock.enitity.Stock;
import com.back.together02be.stock.repository.StockRepository;
import com.back.together02be.trade.repository.TradeRepository;
import com.back.together02be.users.entity.Users;
import com.back.together02be.users.repository.UsersRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.post;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TradeControllerSellConcurrencyTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserStockRepository userStockRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private StockRepository stockRepository;

    private Users users;
    private Stock stock;

    // ────────────────────────────────────────────
    // 각 테스트마다 깨끗한 데이터 세팅
    // @Transactional 사용 X → 스레드간 커밋이 실제로 보여야 함
    // ────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        // 역순 삭제 (FK 제약조건)
        tradeRepository.deleteAll();
        userStockRepository.deleteAll();
        userAccountRepository.deleteAll();
        usersRepository.deleteAll();
        stockRepository.deleteAll();

        users = usersRepository.save(new Users("testuser", "password123", "테스터"));
        stock = stockRepository.save(new Stock("005930", "삼성전자", "KOSPI"));
    }

    @AfterEach
    void tearDown() {
        tradeRepository.deleteAll();
        userStockRepository.deleteAll();
        userAccountRepository.deleteAll();
        usersRepository.deleteAll();
        stockRepository.deleteAll();
    }

    // ────────────────────────────────────────────
    // 동시성 테스트
    // ────────────────────────────────────────────

    @Test
    @DisplayName("동시성 - 동시에 부분 매도 2건 → 둘 다 성공, 수량 정확히 차감")
    void t1() throws Exception {
        // given: 10주 보유, 5주씩 2번 동시 매도 → 둘 다 성공해야 함
        userStockRepository.save(new UserStock(users, stock, 10L, 70000L));
        userAccountRepository.save(new UserAccount(users, 700000L, 1000000L));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount); // 스레드 준비 대기
        CountDownLatch start = new CountDownLatch(1);           // 동시 출발 신호
        CountDownLatch done  = new CountDownLatch(threadCount); // 완료 대기

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        String requestBody = """
                {
                    "userId": %d,
                    "stockId": %d,
                    "quantity": 5,
                    "price": 75000
                }
                """.formatted(users.getId(), stock.getId());

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ready.countDown();  // 준비 완료
                    start.await();      // 출발 신호 대기 (동시 출발)

                    mvc.perform(
                            post("/api/trades/sell")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    ).andDo(result -> {
                        if (result.getResponse().getStatus() == 200) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    });
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();       // 모든 스레드 준비될 때까지 대기
        start.countDown();   // 동시 출발
        done.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(2); // 둘 다 성공
        assertThat(failCount.get()).isEqualTo(0);

        // DB 수량 검증: 10 - 5 - 5 = 0 → UserStock 삭제됨
        Optional<UserStock> remaining =
                userStockRepository.findByUsersIdAndStockId(users.getId(), stock.getId());
        assertThat(remaining).isEmpty();

        // 예수금 검증: 100만 + (75000*5) + (75000*5) = 1,750,000
        UserAccount userAccount =
                userAccountRepository.findByUsersId(users.getId()).orElseThrow();
        assertThat(userAccount.getDeposit()).isEqualTo(1750000L);

        // 거래 내역 2건 저장됐는지 검증
        assertThat(tradeRepository.findAll().size()).isEqualTo(2);
    }

    @Test
    @DisplayName("동시성 - 전량 중복 매도 2건 → 1건 성공, 1건 실패 (잔고 마이너스 불가)")
    void t2() throws Exception {
        // given: 10주 보유, 10주씩 2번 동시 매도 → 1건만 성공해야 함
        userStockRepository.save(new UserStock(users, stock, 10L, 70000L));
        userAccountRepository.save(new UserAccount(users, 700000L, 1000000L));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        String requestBody = """
                {
                    "userId": %d,
                    "stockId": %d,
                    "quantity": 10,
                    "price": 75000
                }
                """.formatted(users.getId(), stock.getId());

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();

                    mvc.perform(
                            post("/api/trades/sell")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    ).andDo(result -> {
                        if (result.getResponse().getStatus() == 200) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    });
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(1); // 1건만 성공
        assertThat(failCount.get()).isEqualTo(1);    // 1건은 수량 부족으로 실패

        // DB 수량이 음수가 되지 않았는지 검증 (핵심!)
        Optional<UserStock> remaining = userStockRepository
                .findByUsersIdAndStockId(users.getId(), stock.getId());
        assertThat(remaining).isEmpty(); // 성공한 1건으로 전량 매도 → 삭제

        // 예수금이 2배로 증가하지 않았는지 검증 (핵심!)
        UserAccount userAccount = userAccountRepository
                .findByUsersId(users.getId()).orElseThrow();
        assertThat(userAccount.getDeposit()).isEqualTo(1750000L); // 100만 + 75000*10 (1번만)

        // 거래 내역 1건만 저장됐는지 검증
        assertThat(tradeRepository.findAll().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("동시성 - 100건 동시 매도 요청 → 보유 수량만큼만 성공")
    void t3() throws Exception {
        // given: 10주 보유, 1주씩 100번 동시 요청 → 10건만 성공
        userStockRepository.save(new UserStock(users, stock, 10L, 70000L));
        userAccountRepository.save(new UserAccount(users, 700000L, 1000000L));

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        String requestBody = """
                {
                    "userId": %d,
                    "stockId": %d,
                    "quantity": 1,
                    "price": 75000
                }
                """.formatted(users.getId(), stock.getId());

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();

                    mvc.perform(
                            post("/api/trades/sell")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    ).andDo(result -> {
                        if (result.getResponse().getStatus() == 200) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    });
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await(30, TimeUnit.SECONDS); // 100개라 여유있게
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(10);  // 보유 수량만큼만 성공
        assertThat(failCount.get()).isEqualTo(90);     // 나머지는 실패

        // 수량이 절대 음수가 되지 않았는지 검증 (핵심!)
        Optional<UserStock> remaining = userStockRepository
                .findByUsersIdAndStockId(users.getId(), stock.getId());
        assertThat(remaining).isEmpty(); // 10주 전량 소진 → 삭제

        // 예수금 검증: 100만 + 75000*10 = 1,750,000
        UserAccount userAccount = userAccountRepository
                .findByUsersId(users.getId()).orElseThrow();
        assertThat(userAccount.getDeposit()).isEqualTo(1750000L);

        // 거래 내역 정확히 10건
        assertThat(tradeRepository.findAll().size()).isEqualTo(10);
    }
}
