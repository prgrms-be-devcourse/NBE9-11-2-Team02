package com.back.together02be.stock.client;

import com.back.together02be.stock.dto.KisPriceRes;
import com.back.together02be.stock.dto.KisTokenRes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class KisPriceClient {

    // application-local.yml에 넣어둔 값 읽어오기
    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.rest-base-url}")
    private String restBaseUrl;

    private final RestClient restClient = RestClient.create();

    private String cachedToken;
    private long tokenIssuedAt;

    public synchronized String getAccessToken() {

        // 1분 이내 발급된 토큰이면 재사용
        if (cachedToken != null && (System.currentTimeMillis() - tokenIssuedAt) < 60_000) {
            return cachedToken;
        }

        // 한투 토큰 발급 URL
        String url = restBaseUrl + "/oauth2/tokenP";

        // 요청 body
        Map<String, String> requestBody = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret
        );

        // POST 요청으로 토큰 발급
        KisTokenRes tokenResponse = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(KisTokenRes.class);

        if (tokenResponse == null || tokenResponse.accessToken() == null) {
            throw new IllegalStateException("토큰 발급 실패");
        }

        cachedToken = tokenResponse.accessToken();
        tokenIssuedAt = System.currentTimeMillis();

        return cachedToken;
    }

    // 토큰 + 종목코드 받아서 -> 한투 API 호출 -> 현재가 데이터 반환
    public KisPriceRes getCurrentPrice(String token, String stockCode) {

        //한투 API 주소 만드는 부분
        String url = restBaseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price"
                + "?fid_cond_mrkt_div_code=J"
                + "&fid_input_iscd=" + stockCode;

        //RestClient로 GET 요청
        KisPriceRes response = restClient.get()
                .uri(url)
                .headers(headers -> {
                    headers.setBearerAuth(token);
                    headers.set("appkey", appKey);
                    headers.set("appsecret", appSecret);
                    headers.set("tr_id", "FHKST01010100");
                })
                //HTTP 응답(JSON) → KisPriceRes 객체로 변환
                .retrieve()
                .body(KisPriceRes.class);

        //예외 처리
        if (response == null || response.output() == null) {
            throw new IllegalStateException("현재가 조회 실패: stockCode=" + stockCode);
        }

        return response;
    }
}