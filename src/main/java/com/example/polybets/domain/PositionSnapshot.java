package com.example.polybets.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Bir kategori senkronizasyonu sırasında, o kategorinin top-20 listesindeki
 * bir cüzdana ait tek bir aktif (henüz redeem edilmemiş) pozisyonun kaydı.
 */
@Entity
@Table(name = "position_snapshot", indexes = {
        @Index(name = "idx_position_category", columnList = "category"),
        @Index(name = "idx_position_condition", columnList = "conditionId")
})
public class PositionSnapshot {

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

    private Double curPrice;

    private Double currentValue;

    private String endDate;

    @Column(nullable = false)
    private Instant syncedAt;

    protected PositionSnapshot() {
        // JPA icin
    }

    public PositionSnapshot(Category category, String proxyWallet, String userName, String conditionId,
                             String marketTitle, String marketSlug, String eventSlug, String outcome,
                             Double curPrice, Double currentValue, String endDate, Instant syncedAt) {
        this.category = category;
        this.proxyWallet = proxyWallet;
        this.userName = userName;
        this.conditionId = conditionId;
        this.marketTitle = marketTitle;
        this.marketSlug = marketSlug;
        this.eventSlug = eventSlug;
        this.outcome = outcome;
        this.curPrice = curPrice;
        this.currentValue = currentValue;
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

    public Double getCurPrice() {
        return curPrice;
    }

    public Double getCurrentValue() {
        return currentValue;
    }

    public String getEndDate() {
        return endDate;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }
}
