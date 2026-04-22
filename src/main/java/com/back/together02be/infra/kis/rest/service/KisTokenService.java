package com.back.together02be.infra.kis.rest.service;

import com.back.together02be.infra.kis.rest.dto.KisTokenRes;
import com.back.together02be.infra.kis.rest.entity.KisAccessToken;
import com.back.together02be.infra.kis.rest.repository.KisAccessTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KisTokenService {

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.rest-base-url}")
    private String restBaseUrl;

    private final KisAccessTokenRepository kisAccessTokenRepository;

    // RestClient는 별도 Bean 없이 현재 구조 유지
    private final RestClient restClient = RestClient.create();

    // 현재 사용 가능한 access token을 반환한다.
    @Transactional
    public synchronized String getAccessToken() {
        KisAccessToken savedToken = kisAccessTokenRepository.findTopByOrderByIdDesc()
                .orElse(null);

        if (savedToken != null && savedToken.isUsable()) {
            log.info("KIS 접근 토큰 재사용. expiresAt={}", savedToken.getExpiresAt());
            return savedToken.getAccessToken();
        }

        return issueAndSaveNewToken();
    }

    // 새 토큰을 발급받아 DB에 저장한다.
    @Transactional
    protected String issueAndSaveNewToken() {
        String url = restBaseUrl + "/oauth2/tokenP";

        Map<String, String> requestBody = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret
        );

        KisTokenRes tokenResponse = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(KisTokenRes.class);

        if (tokenResponse == null || tokenResponse.accessToken() == null) {
            throw new IllegalStateException("토큰 발급 실패");
        }

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(tokenResponse.expiresIn() == null ? 0 : tokenResponse.expiresIn());

        KisAccessToken tokenEntity = kisAccessTokenRepository.findTopByOrderByIdDesc()
                .orElse(null);

        if (tokenEntity == null) {
            tokenEntity = new KisAccessToken(
                    tokenResponse.accessToken(),
                    tokenResponse.tokenType(),
                    expiresAt
            );
        } else {
            tokenEntity.update(
                    tokenResponse.accessToken(),
                    tokenResponse.tokenType(),
                    expiresAt
            );
        }

        kisAccessTokenRepository.save(tokenEntity);
        log.info("KIS 접근 토큰 신규 발급 및 저장 완료. expiresAt={}", expiresAt);

        return tokenEntity.getAccessToken();
    }
}