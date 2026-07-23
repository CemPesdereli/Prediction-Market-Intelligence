package com.example.polybets.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Bir kategori için son senkronizasyonda çekilen leaderboard satırının cache kaydı.
 */
@Entity
@Table(name = "leaderboard_entry", indexes = {
        @Index(name = "idx_leaderboard_category", columnList = "category")
})
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false)
    private Integer rank;

    @Column(nullable = false, length = 64)
    private String proxyWallet;

    @Column(length = 100)
    private String userName;

    private Double vol;

    private Double pnl;

    @Column(nullable = false)
    private Instant syncedAt;

    protected LeaderboardEntry() {
        // JPA icin
    }

    public LeaderboardEntry(Category category, Integer rank, String proxyWallet, String userName,
                             Double vol, Double pnl, Instant syncedAt) {
        this.category = category;
        this.rank = rank;
        this.proxyWallet = proxyWallet;
        this.userName = userName;
        this.vol = vol;
        this.pnl = pnl;
        this.syncedAt = syncedAt;
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public Integer getRank() {
        return rank;
    }

    public String getProxyWallet() {
        return proxyWallet;
    }

    public String getUserName() {
        return userName;
    }

    public Double getVol() {
        return vol;
    }

    public Double getPnl() {
        return pnl;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }
}
