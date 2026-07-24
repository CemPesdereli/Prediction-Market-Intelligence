package com.example.polybets.service;

import com.example.polybets.domain.Category;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Her N dakikada bir (application.yml: polymarket.sync.cron) ve uygulama ayağa
 * kalktığında {@link LeaderboardSyncService#syncCategory} metodunu her kategori
 * için tetikler.
 *
 * Bu tetikleme mantığı bilinçli olarak LeaderboardSyncService'ten ayrı bir bean'de:
 * syncCategory @Transactional oldugu icin dışarıdan (proxy uzerinden) cagrilmali;
 * ayni sinif icinde self-invocation olsaydi @Transactional atlanirdi.
 */
@Component
public class LeaderboardSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardSyncScheduler.class);

    private final LeaderboardSyncService syncService;
    private final boolean syncEnabled;

    public LeaderboardSyncScheduler(
            LeaderboardSyncService syncService,
            @Value("${polymarket.sync.enabled}") boolean syncEnabled) {
        this.syncService = syncService;
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
            syncService.syncCategory(category);
        }
    }

    @Scheduled(cron = "${polymarket.sync.cron}")
    public void scheduledSync() {
        if (!syncEnabled) {
            return;
        }
        log.info("Zamanlanmis senkronizasyon basliyor...");
        for (Category category : Category.values()) {
            syncService.syncCategory(category);
        }
        log.info("Zamanlanmis senkronizasyon tamamlandi.");
    }
}
