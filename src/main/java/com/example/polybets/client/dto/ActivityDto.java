package com.example.polybets.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * GET https://data-api.polymarket.com/activity?user=0x...&type=REDEEM yanıtındaki
 * tek bir on-chain aktivite kaydı. /positions?redeemable=true'nun aksine bu on-chain
 * geçmiş olduğu için, kullanıcı kazandığı pozisyonu redeem edip claim ettikten sonra
 * da burada görünmeye devam eder.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ActivityDto(
        String proxyWallet,
        Long timestamp,
        String conditionId,
        String type,
        Double size,
        Double usdcSize,
        Double price,
        String asset,
        String side,
        Integer outcomeIndex,
        String title,
        String slug,
        String eventSlug,
        String outcome
) {
}
