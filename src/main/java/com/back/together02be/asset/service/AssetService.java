package com.back.together02be.asset.service;

import com.back.together02be.asset.dto.response.UserStockRes;
import com.back.together02be.asset.repository.UserStockRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final UserStockRepository userStockRepository;

    public List<UserStockRes> getUserStocks(Long userId) {
        return userStockRepository.findAllByUsersId(userId).stream()
                .map(UserStockRes::from)
                .toList();
    }
}
