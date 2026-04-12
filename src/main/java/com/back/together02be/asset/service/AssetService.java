package com.back.together02be.asset.service;

import com.back.together02be.asset.enitity.UserAccount;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.users.enitity.Users;
import org.hibernate.service.spi.ServiceException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Member;
import java.nio.file.AccessDeniedException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final UserAccountRepository userAccountRepository;
    public long getTotalAmountByUserId(long userId){
        return userAccountRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("계좌 없음"))
                .getTotalPurchase();
    }
}
