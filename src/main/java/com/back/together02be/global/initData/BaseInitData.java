package com.back.together02be.global.initData;

import com.back.together02be.asset.entity.UserAccount;
import com.back.together02be.asset.entity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.stock.entity.Stock;
import com.back.together02be.stock.repository.StockRepository;
import com.back.together02be.users.dto.request.SignupReq;
import com.back.together02be.users.entity.Users;
import com.back.together02be.users.repository.UsersRepository;
import com.back.together02be.users.service.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class BaseInitData {

    private final UsersRepository usersRepository;
    private final StockRepository stockRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserStockRepository userStockRepository;

    @Autowired
    @Lazy
    private BaseInitData self;

    private final UsersService usersService;

    @Bean
    public ApplicationRunner initData() {
        return args -> {
            self.work1();
            self.work2();
            self.work3();
        };
    }

    @Transactional
    public void work1() {
        // 테스트용 시드 데이터 (H2 dev 환경 전용)
        Users user = usersRepository.save(new Users("testuser", "password", "테스트유저"));
        //Stock stock = stockRepository.save(new Stock("005930", "삼성전자", "KOSPI"));
        userAccountRepository.save(new UserAccount(user, 0L, 50_000_000L));

    }

    @Transactional
    public void work2() {
		if (usersService.count() > 0) {
			return;
		}

        usersService.signup(new SignupReq("user1", "1234", "1234", "유저1"));
        usersService.signup(new SignupReq("user2", "1234", "1234", "유저2"));
        usersService.signup(new SignupReq("user3", "1234", "1234", "유저3"));
        Users user = usersRepository.findByUsername("user1")
                .orElseGet(() -> usersRepository.save(new Users("user1", "1234", "유저1")));
        userAccountRepository.save(new UserAccount(user, 0L, 50_000_000L));
    }

    @Transactional
    public void work3() {
        // 1. 기존에 생성한 유저와 주식을 가져오거나 새로 생성
        Users user = usersRepository.findByUsername("user1")
                .orElseGet(() -> usersRepository.save(new Users("user1", "1234", "유저1")));

        Stock stock = stockRepository.findByStockCode("005930")
                .orElseGet(() -> stockRepository.save(new Stock("005930", "삼성전자", "KOSPI")));

        // 2. 해당 유저가 이미 이 주식을 가지고 있는지 확인 (중복 에러 방지)
        if (userStockRepository.findByUsersIdAndStockId(user.getId(), stock.getId())!=null) {
            // 보유 주식 10주, 평균단가 70,000원으로 초기 데이터 생성
            UserStock userStock = new UserStock(user, stock, 10L, 70_000L);
            userStockRepository.save(userStock);
        }
    }

}
