package com.example.polybets.service;

import com.example.polybets.client.PolymarketDataApiClient;
import com.example.polybets.client.dto.ActivityDto;
import com.example.polybets.client.dto.LeaderboardEntryDto;
import com.example.polybets.client.dto.PositionDto;
import com.example.polybets.domain.Category;
import com.example.polybets.domain.ClosedPositionSnapshot;
import com.example.polybets.domain.LeaderboardEntry;
import com.example.polybets.domain.PositionSnapshot;
import com.example.polybets.repository.ClosedPositionSnapshotRepository;
import com.example.polybets.repository.LeaderboardEntryRepository;
import com.example.polybets.repository.PositionSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tek bir kategori için Polymarket'ten leaderboard + pozisyon verisini çeker ve
 * H2'ye cache olarak yazar (bir önceki senkronizasyonun kayıtlarının yerine).
 *
 * Zamanlama/tetikleme {@link LeaderboardSyncScheduler}'da: syncCategory buradan
 * sadece dışarıdan (baska bir bean uzerinden) cagrilmali ki @Transactional proxy
 * uzerinden calissin (self-invocation @Transactional'i atlar).
 */
@Service
public class LeaderboardSyncService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardSyncService.class);

    private final PolymarketDataApiClient apiClient;
    private final LeaderboardEntryRepository leaderboardRepository;
    private final PositionSnapshotRepository positionRepository;
    private final ClosedPositionSnapshotRepository closedPositionRepository;
    private final int topN;
    private final int closedWindowDays;

    public LeaderboardSyncService(
            PolymarketDataApiClient apiClient,
            LeaderboardEntryRepository leaderboardRepository,
            PositionSnapshotRepository positionRepository,
            ClosedPositionSnapshotRepository closedPositionRepository,
            @Value("${polymarket.top-n}") int topN,
            @Value("${polymarket.closed-window-days}") int closedWindowDays) {
        this.apiClient = apiClient;
        this.leaderboardRepository = leaderboardRepository;
        this.positionRepository = positionRepository;
        this.closedPositionRepository = closedPositionRepository;
        this.topN = topN;
        this.closedWindowDays = closedWindowDays;
    }

    /**
     * Tek bir kategori için leaderboard + pozisyon verisini çeker ve DB'yi günceller.
     */
    @Transactional
    public void syncCategory(Category category) {
        List<LeaderboardEntryDto> leaderboard = apiClient.getMonthlyLeaderboard(category, topN);
        if (leaderboard.isEmpty()) {
            log.warn("Kategori {} icin leaderboard bos geldi, mevcut cache korunuyor.", category);
            return;
        }

        Instant now = Instant.now();
        LocalDate today = LocalDate.now();
        LocalDate closedCutoff = today.minusDays(closedWindowDays);

        leaderboardRepository.deleteByCategory(category);
        positionRepository.deleteByCategory(category);
        closedPositionRepository.deleteByCategory(category);

        int rank = 1;
        for (LeaderboardEntryDto entry : leaderboard) {
            leaderboardRepository.save(new LeaderboardEntry(
                    category, rank++, entry.proxyWallet(), entry.userName(),
                    entry.vol(), entry.pnl(), now));

            List<PositionDto> activePositions = apiClient.getActivePositions(entry.proxyWallet());
            for (PositionDto pos : activePositions) {
                if (pos.conditionId() == null) {
                    continue;
                }
                positionRepository.save(new PositionSnapshot(
                        category,
                        entry.proxyWallet(),
                        entry.userName(),
                        pos.conditionId(),
                        pos.title(),
                        pos.slug(),
                        pos.eventSlug(),
                        pos.outcome(),
                        pos.curPrice(),
                        pos.currentValue(),
                        pos.endDate(),
                        now));
            }

            Set<String> closedConditionIds = new HashSet<>();

            List<PositionDto> closedPositions = apiClient.getClosedPositions(entry.proxyWallet());
            for (PositionDto pos : closedPositions) {
                if (pos.conditionId() == null || !isWithinClosedWindow(pos.endDate(), closedCutoff, today)) {
                    continue;
                }
                boolean won = pos.curPrice() != null && pos.curPrice() >= 0.5;
                String resolvedOutcome = won ? pos.outcome() : pos.oppositeOutcome();
                closedPositionRepository.save(new ClosedPositionSnapshot(
                        category,
                        entry.proxyWallet(),
                        entry.userName(),
                        pos.conditionId(),
                        pos.title(),
                        pos.slug(),
                        pos.eventSlug(),
                        pos.outcome(),
                        won,
                        resolvedOutcome,
                        pos.cashPnl(),
                        pos.endDate(),
                        now));
                closedConditionIds.add(pos.conditionId());
            }

            // Kazanan pozisyonlar redeem edilir edilmez /positions?redeemable=true
            // listesinden dusuyor. Bunlari yakalamak icin, ayni pencerede yapilmis
            // REDEEM aktivitesine (on-chain, redeem sonrasi da kalici) bakiyoruz.
            long cutoffEpochSeconds = closedCutoff.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            List<ActivityDto> redeemActivity = apiClient.getRedeemActivity(entry.proxyWallet(), cutoffEpochSeconds);
            for (ActivityDto act : redeemActivity) {
                if (act.conditionId() == null || !closedConditionIds.add(act.conditionId())) {
                    continue;
                }
                closedPositionRepository.save(new ClosedPositionSnapshot(
                        category,
                        entry.proxyWallet(),
                        entry.userName(),
                        act.conditionId(),
                        act.title(),
                        act.slug(),
                        act.eventSlug(),
                        act.outcome(),
                        true,
                        act.outcome(),
                        null,
                        act.timestamp() != null
                                ? DateTimeFormatter.ISO_LOCAL_DATE.format(
                                        Instant.ofEpochSecond(act.timestamp()).atZone(ZoneOffset.UTC))
                                : null,
                        now));
            }
        }
        log.info("Kategori {} senkronize edildi: {} kullanici.", category, leaderboard.size());
    }

    /**
     * endDate "YYYY-MM-DD" formatinda gelir (bkz. Polymarket /positions yaniti).
     * Parse edilemeyen ya da olculu aralik disindaki kayitlar sessizce atlanir.
     */
    private boolean isWithinClosedWindow(String endDate, LocalDate cutoff, LocalDate today) {
        if (endDate == null) {
            return false;
        }
        try {
            LocalDate parsed = LocalDate.parse(endDate.length() >= 10 ? endDate.substring(0, 10) : endDate);
            return !parsed.isBefore(cutoff) && !parsed.isAfter(today);
        } catch (Exception e) {
            return false;
        }
    }
}
