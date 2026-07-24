package com.example.polybets.service;

import com.example.polybets.client.PolymarketDataApiClient;
import com.example.polybets.client.dto.LeaderboardEntryDto;
import com.example.polybets.client.dto.PositionDto;
import com.example.polybets.domain.Category;
import com.example.polybets.domain.LeaderboardEntry;
import com.example.polybets.domain.PositionSnapshot;
import com.example.polybets.repository.LeaderboardEntryRepository;
import com.example.polybets.repository.PositionSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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
    private final int topN;

    public LeaderboardSyncService(
            PolymarketDataApiClient apiClient,
            LeaderboardEntryRepository leaderboardRepository,
            PositionSnapshotRepository positionRepository,
            @Value("${polymarket.top-n}") int topN) {
        this.apiClient = apiClient;
        this.leaderboardRepository = leaderboardRepository;
        this.positionRepository = positionRepository;
        this.topN = topN;
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

        leaderboardRepository.deleteByCategory(category);
        positionRepository.deleteByCategory(category);

        int rank = 1;
        for (LeaderboardEntryDto entry : leaderboard) {
            leaderboardRepository.save(new LeaderboardEntry(
                    category, rank++, entry.proxyWallet(), entry.userName(),
                    entry.vol(), entry.pnl(), now));

            List<PositionDto> positions = apiClient.getActivePositions(entry.proxyWallet());
            for (PositionDto pos : positions) {
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
        }
        log.info("Kategori {} senkronize edildi: {} kullanici.", category, leaderboard.size());
    }
}
