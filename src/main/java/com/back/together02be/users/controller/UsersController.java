package com.back.together02be.users.controller;

import com.back.together02be.global.apiRes.ApiRes;
import com.back.together02be.users.dto.request.LoginReq;
import com.back.together02be.users.dto.request.UsersReq;
import com.back.together02be.users.dto.response.UsersRes;
import com.back.together02be.users.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "UsersController", description = "유저 API")
public class UsersController {

    private final UsersService usersService;

    @PostMapping("/signup")
    @Operation(summary = "회원 가입")
    public ResponseEntity<ApiRes<Void>> signup(@RequestBody UsersReq req) {
        usersService.signup(req);
        return ResponseEntity.ok(new ApiRes<>("회원가입 성공", null));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인")
    public ResponseEntity<ApiRes<UsersRes>> login(@RequestBody LoginReq req) {
        String[] tokens = usersService.login(req);
        return ResponseEntity.ok(new ApiRes<>("로그인 성공", new UsersRes(tokens[0], tokens[1])));
    }
}
