package com.back.together02be.global.initData;

import com.back.together02be.asset.enitity.UserAccount;
import com.back.together02be.asset.enitity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.stock.enitity.Stock;
import com.back.together02be.stock.repository.StockRepository;
import com.back.together02be.users.entity.Users;
import com.back.together02be.users.repository.UsersRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test") // 테스트 환경에서만 실행되도록 제한
public class TestInitData {

    @Autowired
    @Lazy
    private TestInitData self;

    @Autowired private UsersRepository userRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private UserStockRepository userStockRepository;

    @Bean
    public ApplicationRunner init() {
        return args -> {
            self.work1(); // 자기 자신(self)을 호출해야 @Transactional이 먹힙니다.
        };
    }

    @Transactional
    public void work1() {
        // 1. 유저 (ID: 1)
        Users user = userRepository.save(new Users("testuser", "password123", "테스터"));

        // 2. 종목 (ID: 1)
        Stock stock = stockRepository.save(new Stock("005930", "삼성전자", "KOSPI"));

        // 3. 계좌 (예수금 100만, 총매입 70만)
        userAccountRepository.save(new UserAccount(user, 700000L, 1000000L));

        // 4. 보유 주식 (10주, 평단가 7만)
        userStockRepository.save(new UserStock(user, stock, 10L, 70000L));
    }
}