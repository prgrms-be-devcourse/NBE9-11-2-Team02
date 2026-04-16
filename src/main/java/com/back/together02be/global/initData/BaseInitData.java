package com.back.together02be.global.initData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

import com.back.together02be.users.dto.request.SignupReq;
import com.back.together02be.users.service.UsersService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class BaseInitData {

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
	}

	@Transactional
	public void work2() {
		if (usersService.count() > 0) return;

		usersService.signup(new SignupReq("user1", "1234", "1234", "유저1"));
		usersService.signup(new SignupReq("user2", "1234", "1234", "유저2"));
		usersService.signup(new SignupReq("user3", "1234", "1234", "유저3"));
	}

}
