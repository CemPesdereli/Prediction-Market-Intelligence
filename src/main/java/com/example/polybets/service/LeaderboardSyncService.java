package com.example.polybets.service;

import com.example.polybets.client.PolymarketDataApiClient;
import com.example.polybets.client.dto.LeaderboardEntryDto;
import com.example.polybets.client.dto.PositionDto;
import com.example.polybets.domain.Category;
import com.example.polybets.domain.LeaderboardEntry;
import com.example.polybets.domain.PositionSnapshot;
import com.example.polybets.repository.LeaderboardEntryRepository;
import com.example.polybets.repository.PositionSnapshotRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Her N dakikada bir (application.yml: polymarket.sync.cron) tanımlı her kategori için:
 *   1) O kategorinin aylık leaderboard'unun ilk top-n kullanıcısını çeker,
 *   2) Her kullanıcının şu anki aktif (redeemable=false) pozisyonlarını çeker,
 *   3) Sonucu H2'ye cache olarak yazar (bir önceki senkronizasyonun kayıtlarının yerine).
 *
 * Böylece web sayfası / REST API her istekte Polymarket'e gitmez, DB'den okur.
 */
@Service
public class LeaderboardSyncService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardSyncService.class);

    private final PolymarketDataApiClient apiClient;
    private final LeaderboardEntryRepository leaderboardRepository;
    private final PositionSnapshotRepository positionRepository;
    private final int topN;
    private final boolean syncEnabled;

    public LeaderboardSyncService(
            PolymarketDataApiClient apiClient,
            LeaderboardEntryRepository leaderboardRepository,
            PositionSnapshotRepository positionRepository,
            @Value("${polymarket.top-n}") int topN,
            @Value("${polymarket.sync.enabled}") boolean syncEnabled) {
        this.apiClient = apiClient;
        this.leaderboardRepository = leaderboardRepository;
        this.positionRepository = positionRepository;
        this.topN = topN;
        this.syncEnabled = syncEnabled;
    }

    /**
     * Uygulama ayağa kalktığında en azından varsayılan kategori için bir kez veri
     * dolu olsun diye ilk senkronizasyonu tetikler.
     */
    @PostConstruct
    public void initialSync() {
        if (!syncEnabled) {
            log.info("Sync devre disi (polymarket.sync.enabled=false).");
            return;
        }
        for (Category category : Category.values()) {
            syncCategory(category);
        }
    }

    @Scheduled(cron = "${polymarket.sync.cron}")
    public void scheduledSync() {
        if (!syncEnabled) {
            return;
        }
        log.info("Zamanlanmis senkronizasyon basliyor...");
        for (Category category : Category.values()) {
            syncCategory(category);
        }
        log.info("Zamanlanmis senkronizasyon tamamlandi.");
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
