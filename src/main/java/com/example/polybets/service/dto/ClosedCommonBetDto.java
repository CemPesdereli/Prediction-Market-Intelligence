package com.example.polybets.service.dto;

import java.util.List;

/**
 * Top-20 icinde en az {minCommonHolders} kisinin ayni piyasada (conditionId)
 * ortak pozisyon tuttugu ve piyasanin son {closedWindowDays} gun icinde
 * sonuclandigi durumda uretilen "kapanmis ortak bahis" kaydi.
 */
public record ClosedCommonBetDto(
        String conditionId,
        String marketTitle,
        String marketSlug,
        String eventSlug,
        String endDate,
        String resolvedOutcome,
        int holderCount,
        List<HolderInfo> holders
) {
    public record HolderInfo(
            String userName,
            String proxyWallet,
            String outcome,
            boolean won,
            Double cashPnl
    ) {
    }
}
