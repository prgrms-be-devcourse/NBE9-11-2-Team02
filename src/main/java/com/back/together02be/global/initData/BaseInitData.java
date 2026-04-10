package com.back.together02be.global.initData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
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
