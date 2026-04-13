package com.back.together02be.stock.client;

import com.back.together02be.stock.dto.KisPriceResponse;
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

    @Value("${kis.rest-base-url}")
    private String restBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private String cachedToken;
    private long tokenIssuedAt;

    public String getAccessToken() {

        if (cachedToken != null && (System.currentTimeMillis() - tokenIssuedAt) < 60_000) {
            return cachedToken;
        }

        //	1.	한투 토큰 발급 URL 만들기
        String url = restBaseUrl + "/oauth2/tokenP";

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

        cachedToken = tokenResponse.access_token();
        tokenIssuedAt = System.currentTimeMillis();

        //	5.	응답에서 access_token 꺼내기
        return tokenResponse.access_token();
    }

    public KisPriceResponse getCurrentPrice(String token, String stockCode) {
        //한투 현재가 조회 주소
        String url = restBaseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price"
                + "?fid_cond_mrkt_div_code=J"
                + "&fid_input_iscd=" + stockCode;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHKST01010100");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<KisPriceResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                KisPriceResponse.class
        );

        KisPriceResponse body = response.getBody();

        if (body == null || body.output() == null) {
            throw new IllegalStateException("현재가 조회 실패: stockCode=" + stockCode);
        }

        return body;
    }}