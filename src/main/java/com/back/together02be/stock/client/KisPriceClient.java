package com.back.together02be.stock.client;

import com.back.together02be.stock.dto.KisTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class KisPriceClient {

    //application-local.yml에 넣어둔 값 읽어오기
    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAccessToken() {
        //	1.	한투 토큰 발급 URL 만들기
        String url = baseUrl + "/oauth2/tokenP";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        //	2.	요청 헤더 설정
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret
        );

        //	3.	요청 body 설정
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<KisTokenResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                KisTokenResponse.class
        );

        //	4.	POST 요청 보내기
        KisTokenResponse tokenResponse = response.getBody();

        if (tokenResponse == null || tokenResponse.access_token() == null) {
            throw new IllegalStateException("토큰 발급 실패");
        }

        //	5.	응답에서 access_token 꺼내기
        return tokenResponse.access_token();
    }
}