package com.back.together02be.global.initData;

import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.stock.repository.StockRepository;
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
        };
    }

    @Transactional
    public void work1() {
    }

}
