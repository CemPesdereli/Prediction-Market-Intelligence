package com.example.polybets.service.dto;

import java.util.List;

/**
 * Top-20 icinde en az {minCommonHolders} kisinin ayni piyasada (conditionId)
 * aktif pozisyonu oldugunda uretilen "ortak bahis" kaydi.
 */
public record CommonBetDto(
        String conditionId,
        String marketTitle,
        String marketSlug,
        String eventSlug,
        String endDate,
        int holderCount,
        List<HolderInfo> holders
) {
    public record HolderInfo(
            String userName,
            String proxyWallet,
            String outcome,
            Double curPrice,
            Double currentValue
    ) {
    }
}
