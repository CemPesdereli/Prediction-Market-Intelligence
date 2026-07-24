package com.example.polybets.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Bir kategori senkronizasyonu sırasında, o kategorinin top-20 listesindeki bir
 * cüzdana ait, piyasası sonuçlanmış (son {polymarket.closed-window-days} gün
 * içinde bitmiş) bir pozisyonun kaydı. Kazanan pozisyonlar genelde hızla redeem
 * edilip Polymarket'in kendi listesinden düştüğü için burada ağırlıklı olarak
 * henüz redeem edilmemiş kayıtlar görülür; bu, harici API'nin doğal bir sınırıdır.
 */
@Entity
@Table(name = "closed_position_snapshot", indexes = {
        @Index(name = "idx_closed_position_category", columnList = "category"),
        @Index(name = "idx_closed_position_condition", columnList = "conditionId")
})
public class ClosedPositionSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false, length = 64)
    private String proxyWallet;

    @Column(length = 100)
    private String userName;

    @Column(nullable = false, length = 100)
    private String conditionId;

    @Column(length = 300)
    private String marketTitle;

    @Column(length = 200)
    private String marketSlug;

    @Column(length = 200)
    private String eventSlug;

    @Column(length = 50)
    private String outcome;

    @Column(nullable = false)
    private Boolean won;

    @Column(length = 50)
    private String resolvedOutcome;

    private Double cashPnl;

    private String endDate;

    @Column(nullable = false)
    private Instant syncedAt;

    protected ClosedPositionSnapshot() {
        // JPA icin
    }

    public ClosedPositionSnapshot(Category category, String proxyWallet, String userName, String conditionId,
                                   String marketTitle, String marketSlug, String eventSlug, String outcome,
                                   Boolean won, String resolvedOutcome, Double cashPnl, String endDate,
                                   Instant syncedAt) {
        this.category = category;
        this.proxyWallet = proxyWallet;
        this.userName = userName;
        this.conditionId = conditionId;
        this.marketTitle = marketTitle;
        this.marketSlug = marketSlug;
        this.eventSlug = eventSlug;
        this.outcome = outcome;
        this.won = won;
        this.resolvedOutcome = resolvedOutcome;
        this.cashPnl = cashPnl;
        this.endDate = endDate;
        this.syncedAt = syncedAt;
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getProxyWallet() {
        return proxyWallet;
    }

    public String getUserName() {
        return userName;
    }

    public String getConditionId() {
        return conditionId;
    }

    public String getMarketTitle() {
        return marketTitle;
    }

    public String getMarketSlug() {
        return marketSlug;
    }

    public String getEventSlug() {
        return eventSlug;
    }

    public String getOutcome() {
        return outcome;
    }

    public Boolean getWon() {
        return won;
    }

    public String getResolvedOutcome() {
        return resolvedOutcome;
    }

    public Double getCashPnl() {
        return cashPnl;
    }

    public String getEndDate() {
        return endDate;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }
}
