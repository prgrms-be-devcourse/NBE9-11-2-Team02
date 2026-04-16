package com.back.together02be.global.initData;

import com.back.together02be.asset.enitity.UserAccount;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.stock.entity.Stock;
import com.back.together02be.stock.entity.StockMarket;
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

    @Autowired
    @Lazy
    private BaseInitData self;

    private final UsersService usersService;

    @Bean
    public ApplicationRunner initData() {
        return args -> {
            self.work1();
            self.work2();
        };
    }

    @Transactional
    public void work1() {
        // 테스트용 시드 데이터 (H2 dev 환경 전용)
        Users user = usersRepository.save(new Users("testuser", "password", "테스트유저"));
        stockRepository.save(new Stock("005930", "삼성전자", StockMarket.KOSPI));
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
    }

}
