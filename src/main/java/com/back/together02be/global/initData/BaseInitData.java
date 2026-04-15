package com.back.together02be.global.initData;

import com.back.together02be.asset.enitity.UserAccount;
import com.back.together02be.asset.enitity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.stock.enitity.Stock;
import com.back.together02be.stock.repository.StockRepository;
import com.back.together02be.users.entity.Users;
import com.back.together02be.users.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class BaseInitData {

	@Autowired
	@Lazy
	private BaseInitData self;

	@Bean
	//work1 실행하기.
	public ApplicationRunner initData() {
		return args -> {
			self.work1();
		};
	}

	@Transactional
	public void work1() {
	}

}
