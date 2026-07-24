package com.example.polybets.client;

import com.example.polybets.client.dto.LeaderboardEntryDto;
import com.example.polybets.client.dto.PositionDto;
import com.example.polybets.domain.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

/**
 * https://data-api.polymarket.com resmi Data API'sine yapılan çağrıları kapsüller.
 * Auth gerektirmiyor (public endpoint'ler).
 */
@Component
public class PolymarketDataApiClient {

    private static final Logger log = LoggerFactory.getLogger(PolymarketDataApiClient.class);

    private final WebClient webClient;
    private final String timePeriod;
    private final String orderBy;
    private final int positionsLimit;

    public PolymarketDataApiClient(
            WebClient polymarketWebClient,
            @Value("${polymarket.time-period}") String timePeriod,
            @Value("${polymarket.order-by}") String orderBy,
            @Value("${polymarket.positions-limit}") int positionsLimit) {
        this.webClient = polymarketWebClient;
        this.timePeriod = timePeriod;
        this.orderBy = orderBy;
        this.positionsLimit = positionsLimit;
    }

    /**
     * Belirtilen kategori için aylık leaderboard'un ilk {limit} kullanıcısını getirir.
     * GET /v1/leaderboard?category=&timePeriod=MONTH&orderBy=PNL&limit=
     */
    public List<LeaderboardEntryDto> getMonthlyLeaderboard(Category category, int limit) {
        try {
            List<LeaderboardEntryDto> result = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/leaderboard")
                            .queryParam("category", category.name())
                            .queryParam("timePeriod", timePeriod)
                            .queryParam("orderBy", orderBy)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToFlux(LeaderboardEntryDto.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(15))
                    .block();
            return result == null ? List.of() : result;
        } catch (Exception e) {
            log.warn("Leaderboard cekilirken hata olustu (category={}): {}", category, e.getMessage());
            return List.of();
        }
    }

    /**
     * Bir cüzdanın şu anki (henüz redeem edilmemiş / açık) pozisyonlarını getirir.
     * GET /positions?user=&redeemable=false&limit=
     */
    public List<PositionDto> getActivePositions(String proxyWallet) {
        return getPositions(proxyWallet, false);
    }

    /**
     * Bir cüzdanın piyasası sonuçlanmış (redeem edilebilir) pozisyonlarını getirir.
     * Kazanan pozisyonlar genelde hızla redeem edilip listeden düştüğü için burada
     * ağırlıklı olarak henüz redeem edilmemiş kayıtlar görülür; sonuç (kazandı/kaybetti)
     * curPrice alanından (1 = kazandı, 0 = kaybetti) çıkarılır.
     * GET /positions?user=&redeemable=true&limit=
     */
    public List<PositionDto> getClosedPositions(String proxyWallet) {
        return getPositions(proxyWallet, true);
    }

    private List<PositionDto> getPositions(String proxyWallet, boolean redeemable) {
        try {
            List<PositionDto> result = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/positions")
                            .queryParam("user", proxyWallet)
                            .queryParam("redeemable", redeemable)
                            .queryParam("limit", positionsLimit)
                            .build())
                    .retrieve()
                    .bodyToFlux(PositionDto.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(15))
                    .block();
            return result == null ? List.of() : result;
        } catch (Exception e) {
            log.warn("Pozisyonlar cekilirken hata olustu (wallet={}, redeemable={}): {}",
                    proxyWallet, redeemable, e.getMessage());
            return List.of();
        }
    }
}
