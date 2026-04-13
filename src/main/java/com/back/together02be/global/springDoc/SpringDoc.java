package com.back.together02be.global.springDoc;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(info = @Info(title = "모의투자 투게더 API", version = "beta", description = "2차 프로젝트 API"))
public class SpringDoc {

    @Bean
    public GroupedOpenApi stocksApi() { // 예시
        return GroupedOpenApi.builder()
                .group("종목 조회 API")
                .pathsToMatch("/api/stocks/**")
                .build();
    }

    @Bean
    public GroupedOpenApi usersApi() {
        return GroupedOpenApi.builder()
                .group("유저 API")
                .pathsToMatch("/api/users/**")
                .build();
    }

}