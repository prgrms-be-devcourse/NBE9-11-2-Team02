package com.back.together02be.ranking.service;

import com.back.together02be.asset.entity.UserAccount;
import com.back.together02be.asset.entity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.ranking.entity.RankingSeason;
import com.back.together02be.ranking.repository.RankingSeasonRepository;
import com.back.together02be.stock.dto.RealtimeStockPrice;
import com.back.together02be.stock.service.RealTimeStockPriceStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingSeasonService {

    private final RankingSeasonRepository rankingSeasonRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserStockRepository userStockRepository;
    private final RealTimeStockPriceStore realTimeStockPriceStore;

    // 시즌 시작 시 전체 유저의 기준 자산을 새로 저장한다.
    @Transactional
    public void startSeason(LocalDate startDate) {
        List<UserAccount> accounts = userAccountRepository.findAll();

        for (UserAccount account : accounts) {
            long totalAsset = calculateTotalAsset(account);
            RankingSeason season = new RankingSeason(account.getUsers(), totalAsset, startDate);
            rankingSeasonRepository.save(season);
        }
    }

    // 기존 활성 시즌을 모두 종료한다.
    @Transactional
    public void closeSeason(LocalDate endDate) {
        List<RankingSeason> activeSeasons = rankingSeasonRepository.findByActiveTrue();

        for (RankingSeason season : activeSeasons) {
            season.close(endDate);
        }
    }

    // 월말 종료 후 다음 시즌 기준 자산을 다시 생성한다.
    @Transactional
    public void resetSeason(LocalDate endDate, LocalDate nextStartDate) {
        closeSeason(endDate);
        startSeason(nextStartDate);
    }

    // 현재 유저의 활성 시즌 정보를 가져온다.
    @Transactional(readOnly = true)
    public RankingSeason getActiveSeason(Long userId) {
        return rankingSeasonRepository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new IllegalStateException("활성 시즌 정보가 없습니다. userId=" + userId));
    }

    // 현재 총자산 = 예수금 + 보유주식 평가금액이다.
    private long calculateTotalAsset(UserAccount userAccount) {
        List<UserStock> userStocks = userStockRepository.findAllByUsersId(userAccount.getUsers().getId());

        long stockEvaluationAmount = userStocks.stream()
                .mapToLong(this::calculateStockEvaluationAmount)
                .sum();

        return userAccount.getDeposit() + stockEvaluationAmount;
    }

    // 보유 종목 1건의 평가금액을 계산한다.
    private long calculateStockEvaluationAmount(UserStock userStock) {
        String stockCode = userStock.getStock().getStockCode();
        RealtimeStockPrice realtimeStockPrice = realTimeStockPriceStore.get(stockCode);

        long currentPrice = extractCurrentPrice(realtimeStockPrice, userStock.getAveragePrice());

        return currentPrice * userStock.getQuantity();
    }

    // 실시간 가격이 없으면 평균매입가를 대신 사용한다.
    private long extractCurrentPrice(RealtimeStockPrice realtimeStockPrice, Long fallbackPrice) {
        if (realtimeStockPrice == null
                || realtimeStockPrice.getPrice() == null
                || realtimeStockPrice.getPrice().isBlank()) {
            return fallbackPrice;
        }

        try {
            return Long.parseLong(realtimeStockPrice.getPrice());
        } catch (NumberFormatException e) {
            return fallbackPrice;
        }
    }
}