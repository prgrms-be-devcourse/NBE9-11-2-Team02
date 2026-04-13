package com.back.together02be.users.service;

import com.back.together02be.users.dto.request.UsersRequestDto;
import com.back.together02be.users.entity.Users;
import com.back.together02be.users.repository.UsersRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository usersRepository;

    public void signup(UsersRequestDto req) {

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
}