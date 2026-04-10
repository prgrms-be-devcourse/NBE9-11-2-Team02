package com.back.together02be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Together02BeApplication {

	public static void main(String[] args) {
		SpringApplication.run(Together02BeApplication.class, args);
	}

}
