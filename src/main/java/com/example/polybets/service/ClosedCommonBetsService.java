package com.example.polybets.service;

import com.example.polybets.domain.Category;
import com.example.polybets.domain.ClosedPositionSnapshot;
import com.example.polybets.repository.ClosedPositionSnapshotRepository;
import com.example.polybets.service.dto.ClosedCommonBetDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bir kategorinin top-20 leaderboard'undaki kullanicilarin, ayni piyasada (conditionId)
 * ortak pozisyon tuttugu ve piyasanin son birkac gun icinde sonuclandigi
 * ("kapanmis ortak bahisler") durumlari hesaplar. Kim hangi tarafi (Yes/No) secmis,
 * piyasa nasil sonuclanmis ve her kullanicinin kar/zarari ne olmus burada gorunur.
 */
@Service
public class ClosedCommonBetsService {

    private final ClosedPositionSnapshotRepository closedPositionRepository;
    private final int minCommonHolders;

    public ClosedCommonBetsService(
            ClosedPositionSnapshotRepository closedPositionRepository,
            @Value("${polymarket.min-common-holders}") int minCommonHolders) {
        this.closedPositionRepository = closedPositionRepository;
        this.minCommonHolders = minCommonHolders;
    }

    public List<ClosedCommonBetDto> getClosedCommonBets(Category category) {
        List<ClosedPositionSnapshot> snapshots = closedPositionRepository.findByCategory(category);

        Map<String, List<ClosedPositionSnapshot>> byMarket = snapshots.stream()
                .collect(Collectors.groupingBy(ClosedPositionSnapshot::getConditionId, LinkedHashMap::new, Collectors.toList()));

        return byMarket.values().stream()
                .filter(list -> distinctWallets(list).size() >= minCommonHolders)
                .map(this::toDto)
                .sorted(Comparator.comparingInt(ClosedCommonBetDto::holderCount).reversed())
                .collect(Collectors.toList());
    }

    private Set<String> distinctWallets(List<ClosedPositionSnapshot> list) {
        return list.stream().map(ClosedPositionSnapshot::getProxyWallet).collect(Collectors.toSet());
    }

    private ClosedCommonBetDto toDto(List<ClosedPositionSnapshot> positions) {
        ClosedPositionSnapshot first = positions.get(0);

        List<ClosedCommonBetDto.HolderInfo> holders = positions.stream()
                .collect(Collectors.toMap(
                        ClosedPositionSnapshot::getProxyWallet,
                        p -> p,
                        (a, b) -> a.getCashPnl() != null && b.getCashPnl() != null
                                && Math.abs(a.getCashPnl()) >= Math.abs(b.getCashPnl()) ? a : b))
                .values().stream()
                .map(p -> new ClosedCommonBetDto.HolderInfo(
                        p.getUserName(), p.getProxyWallet(), p.getOutcome(),
                        Boolean.TRUE.equals(p.getWon()), p.getCashPnl()))
                .sorted(Comparator.comparing(
                        h -> h.cashPnl() == null ? 0.0 : h.cashPnl(),
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return new ClosedCommonBetDto(
                first.getConditionId(),
                first.getMarketTitle(),
                first.getMarketSlug(),
                first.getEventSlug(),
                first.getEndDate(),
                first.getResolvedOutcome(),
                holders.size(),
                holders);
    }
}
