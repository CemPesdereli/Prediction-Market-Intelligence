package com.example.polybets.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * GET https://data-api.polymarket.com/v1/leaderboard yanıtındaki tek bir satır.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeaderboardEntryDto(
        String rank,
        String proxyWallet,
        String userName,
        Double vol,
        Double pnl,
        String profileImage,
        String xUsername,
        Boolean verifiedBadge
) {
}
