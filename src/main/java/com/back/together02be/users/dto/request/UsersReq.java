package com.back.together02be.users.dto.request;

public record UsersReq(
        String username,
        String password,
        String passwordConfirm,
        String nickname
) {}