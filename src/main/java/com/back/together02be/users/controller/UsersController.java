package com.back.together02be.users.controller;

import com.back.together02be.global.apiRes.ApiRes;
import com.back.together02be.users.dto.request.LoginReq;
import com.back.together02be.users.dto.request.TokenReq;
import com.back.together02be.users.dto.request.SignupReq;
import com.back.together02be.users.dto.response.UsersRes;
import com.back.together02be.users.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "UsersController", description = "유저 API")
public class UsersController {

    private final UsersService usersService;

    @PostMapping("/signup")
    @Operation(summary = "회원 가입")
    public ResponseEntity<ApiRes<Void>> signup(@RequestBody @Valid SignupReq req) {

        usersService.signup(req);
        return ResponseEntity.ok(
                new ApiRes<>(
                        "회원가입 성공",
                        null
                )
        );
    }

    // tokens[0] : Access, tokens[1] : Refresh

    @PostMapping("/login")
    @Operation(summary = "로그인")
    public ResponseEntity<ApiRes<UsersRes>> login(@RequestBody LoginReq req) {

        String[] tokens = usersService.login(req);
        return ResponseEntity.ok(
                new ApiRes<>(
                        "로그인 성공",
                        new UsersRes(tokens[0], tokens[1])
                )
        );
    }

    @PostMapping("/token")
    @Operation(summary = "토큰 재발급")
    public ResponseEntity<ApiRes<UsersRes>> reissueToken(@RequestBody TokenReq req) {

        String[] tokens = usersService.reissueToken(req);
        return ResponseEntity.ok(
                new ApiRes<>(
                        "토큰 재발급 성공",
                        new UsersRes(tokens[0], tokens[1])
                )
        );
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    public ResponseEntity<ApiRes<Void>> logout(@RequestBody TokenReq req) {

        usersService.logout(req);
        return ResponseEntity.ok(
                new ApiRes<>(
                        "로그아웃 성공",
                        null
                )
        );
    }
}
