package com.back.together02be.ranking.service;

import com.back.together02be.asset.entity.UserAccount;
import com.back.together02be.asset.entity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.ranking.entity.Ranking;
import com.back.together02be.ranking.entity.RankingSeason;
import com.back.together02be.ranking.entity.RankingSnapshotType;
import com.back.together02be.ranking.repository.RankingRepository;
import com.back.together02be.stock.dto.RealtimeStockPrice;
import com.back.together02be.stock.service.RealTimeStockPriceStore;
import com.back.together02be.users.entity.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingSnapshotService {

    private final RankingRepository rankingRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserStockRepository userStockRepository;
    private final RealTimeStockPriceStore realTimeStockPriceStore;
    private final RankingSeasonService rankingSeasonService;

    // DAILY 랭킹은 같은 날짜면 덮어쓴다.
    @Transactional
    public void createDailySnapshot(LocalDate snapshotDate) {
        rankingRepository.deleteRankings(RankingSnapshotType.DAILY, snapshotDate);

        List<RankingCandidate> candidates = getTop5Candidates();
        saveRankings(candidates, RankingSnapshotType.DAILY, snapshotDate);

        log.info("DAILY ranking created: {}", snapshotDate);
    }

    // MONTHLY 랭킹은 같은 날짜면 중복 저장하지 않는다.
    @Transactional
    public void createMonthlySnapshot(LocalDate snapshotDate) {
        if (rankingRepository.existsRanking(RankingSnapshotType.MONTHLY, snapshotDate)) {
            throw new IllegalStateException("이미 해당 날짜의 MONTHLY 랭킹이 존재합니다.");
        }

        List<RankingCandidate> candidates = getTop5Candidates();
        saveRankings(candidates, RankingSnapshotType.MONTHLY, snapshotDate);

        log.info("MONTHLY ranking created: {}", snapshotDate);
    }

    // 전체 유저를 수익률 기준으로 정렬해 top5만 뽑는다.
    private List<RankingCandidate> getTop5Candidates() {
        return userAccountRepository.findAll()
                .stream()
                .map(this::toCandidate)
                .sorted(
                        Comparator.comparing(RankingCandidate::profitRate).reversed()
                                .thenComparing(RankingCandidate::totalAsset).reversed()
                                .thenComparing(candidate -> candidate.user().getId())
                )
                .limit(5)
                .toList();
    }

    // 계좌 하나를 랭킹 후보 데이터로 변환한다.
    private RankingCandidate toCandidate(UserAccount userAccount) {
        Users user = userAccount.getUsers();
        List<UserStock> userStocks = userStockRepository.findAllByUsersId(user.getId());

        long stockEvaluationAmount = userStocks.stream()
                .mapToLong(this::calculateStockEvaluationAmount)
                .sum();

        long totalAsset = userAccount.getDeposit() + stockEvaluationAmount;
        RankingSeason season = rankingSeasonService.getActiveSeason(user.getId());
        BigDecimal profitRate = calculateProfitRate(totalAsset, season.getBaseAsset());

        return new RankingCandidate(user, totalAsset, profitRate);
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

    // 수익률은 시즌 기준 자산 대비로 계산한다.
    private BigDecimal calculateProfitRate(long totalAsset, long baseAsset) {
        if (baseAsset <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(totalAsset - baseAsset)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(baseAsset), 2, RoundingMode.HALF_UP);
    }

    // top5 후보를 실제 Ranking 엔티티로 저장한다.
    private void saveRankings(
            List<RankingCandidate> candidates,
            RankingSnapshotType snapshotType,
            LocalDate snapshotDate
    ) {
        int rank = 1;

        for (RankingCandidate candidate : candidates) {
            Ranking ranking = new Ranking(
                    candidate.user(),
                    rank++,
                    candidate.profitRate(),
                    candidate.totalAsset(),
                    snapshotType,
                    snapshotDate
            );

            rankingRepository.save(ranking);
        }
    }

    // 계산 중에만 쓰는 내부 객체이다.
    private record RankingCandidate(
            Users user,
            Long totalAsset,
            BigDecimal profitRate
    ) {
    }
}