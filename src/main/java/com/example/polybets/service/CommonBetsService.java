package com.example.polybets.service;

import com.example.polybets.domain.Category;
import com.example.polybets.domain.PositionSnapshot;
import com.example.polybets.repository.PositionSnapshotRepository;
import com.example.polybets.service.dto.CommonBetDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bir kategorinin top-20 leaderboard'undaki kullanicilarin, ayni piyasada (conditionId)
 * ayni anda aktif pozisyonu oldugu durumlari ("ortak bahisler") hesaplar.
 *
 * Not: Bir piyasada YES / NO gibi farkli taraflarda olsalar dahi ayni market uzerine
 * bahis oynadiklari icin "ortak bahis" olarak sayilir; hangi tarafta olduklari
 * holder listesinde (outcome alani) ayrica gosterilir.
 */
@Service
public class CommonBetsService {

    private final PositionSnapshotRepository positionRepository;
    private final int minCommonHolders;

    public CommonBetsService(
            PositionSnapshotRepository positionRepository,
            @Value("${polymarket.min-common-holders}") int minCommonHolders) {
        this.positionRepository = positionRepository;
        this.minCommonHolders = minCommonHolders;
    }

    public List<CommonBetDto> getCommonBets(Category category) {
        List<PositionSnapshot> snapshots = positionRepository.findByCategory(category);

        Map<String, List<PositionSnapshot>> byMarket = snapshots.stream()
                .collect(Collectors.groupingBy(PositionSnapshot::getConditionId, LinkedHashMap::new, Collectors.toList()));

        return byMarket.values().stream()
                // ayni kullanici ayni markette birden fazla outcome/asset'te pozisyon tutabilir;
                // "ortak bahis" acisindan onemli olan farkli KULLANICI sayisidir.
                .filter(list -> distinctWallets(list).size() >= minCommonHolders)
                .map(this::toDto)
                .sorted(Comparator.comparingInt(CommonBetDto::holderCount).reversed())
                .collect(Collectors.toList());
    }

    private java.util.Set<String> distinctWallets(List<PositionSnapshot> list) {
        return list.stream().map(PositionSnapshot::getProxyWallet).collect(Collectors.toSet());
    }

    private CommonBetDto toDto(List<PositionSnapshot> positions) {
        PositionSnapshot first = positions.get(0);

        List<CommonBetDto.HolderInfo> holders = positions.stream()
                .collect(Collectors.toMap(
                        PositionSnapshot::getProxyWallet,
                        p -> p,
                        (a, b) -> a.getCurrentValue() != null && b.getCurrentValue() != null
                                && a.getCurrentValue() >= b.getCurrentValue() ? a : b))
                .values().stream()
                .map(p -> new CommonBetDto.HolderInfo(
                        p.getUserName(), p.getProxyWallet(), p.getOutcome(), p.getCurPrice(), p.getCurrentValue()))
                .sorted(Comparator.comparing(
                        h -> h.currentValue() == null ? 0.0 : h.currentValue(),
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return new CommonBetDto(
                first.getConditionId(),
                first.getMarketTitle(),
                first.getMarketSlug(),
                first.getEventSlug(),
                first.getEndDate(),
                holders.size(),
                holders);
    }
}
