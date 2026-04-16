package com.back.together02be.users.service;

import com.back.together02be.global.util.JwtUtil;
import com.back.together02be.users.dto.request.LoginReq;
import com.back.together02be.users.dto.request.TokenReq;
import com.back.together02be.users.dto.request.SignupReq;
import com.back.together02be.users.entity.Users;
import com.back.together02be.users.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository usersRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-expire-seconds}")
    private long accessExpireSeconds;

    @Value("${jwt.refresh-expire-seconds}")
    private long refreshExpireSeconds;

    @Transactional
    public void signup(SignupReq req) {

        if (usersRepository.findByUsername(req.username()).isPresent()) {
            throw new IllegalArgumentException("이미 사용중인 아이디입니다.");
        }

        if (!req.password().equals(req.passwordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // db에 [회원가입 한 user] 저장
        Users user = new Users(req.username(), req.password(), req.nickname());
        usersRepository.save(user);
    }

    public long count() {
        return usersRepository.count();
    }

    @Transactional
    public String[] login(LoginReq req) {

        Users user = usersRepository
                .findByUsername(req.username())
                .orElseThrow(
                        () -> new IllegalArgumentException("존재하지 않는 아이디입니다.")
                );

        if (!req.password().equals(user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // AccessToken 발급
        String accessToken = JwtUtil.generateAccessToken(
                jwtSecret,
                accessExpireSeconds,
                Map.of("username", user.getUsername())
        );

        // RefreshToken 발급
        String refreshToken = UUID.randomUUID().toString();
        user.updateRefreshToken(
                refreshToken,
                LocalDateTime.now().plusSeconds(refreshExpireSeconds)
        );

        return new String[]{accessToken, refreshToken};
    }

    @Transactional
    public void logout(TokenReq req) {

        Users user = usersRepository
                .findByRefreshToken(req.refreshToken())
                .orElseThrow(
                        () -> new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.")
                );

        user.clearRefreshToken();
    }

    @Transactional
    public String[] reissueToken(TokenReq req) {

        Users user = usersRepository
                .findByRefreshToken(req.refreshToken())
                .orElseThrow(
                        () -> new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.")
                );

        // 만료시간(10시) < 현재시간(11시)
        if (user.getRefreshTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("리프레시 토큰이 만료되었습니다.");
        }

        // AccessToken 갱신
        String newAccessToken = JwtUtil.generateAccessToken(
                jwtSecret,
                accessExpireSeconds,
                Map.of("username", user.getUsername())
        );

        // RefreshToken 갱신
        String newRefreshToken = UUID.randomUUID().toString();
        user.updateRefreshToken(
                newRefreshToken,
                LocalDateTime.now().plusSeconds(refreshExpireSeconds)
        );

        return new String[]{newAccessToken, newRefreshToken};
    }
}