package com.back.together02be.users.dto.request;

public record SignupReq(
        String username,
        String password,
        String passwordConfirm,
        String nickname
) {}