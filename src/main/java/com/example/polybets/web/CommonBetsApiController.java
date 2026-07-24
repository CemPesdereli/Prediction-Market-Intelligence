package com.example.polybets.web;

import com.example.polybets.domain.Category;
import com.example.polybets.service.ClosedCommonBetsService;
import com.example.polybets.service.CommonBetsService;
import com.example.polybets.service.LeaderboardSyncService;
import com.example.polybets.service.dto.ClosedCommonBetDto;
import com.example.polybets.service.dto.CommonBetDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommonBetsApiController {

    private final CommonBetsService commonBetsService;
    private final ClosedCommonBetsService closedCommonBetsService;
    private final LeaderboardSyncService syncService;

    public CommonBetsApiController(
            CommonBetsService commonBetsService,
            ClosedCommonBetsService closedCommonBetsService,
            LeaderboardSyncService syncService) {
        this.commonBetsService = commonBetsService;
        this.closedCommonBetsService = closedCommonBetsService;
        this.syncService = syncService;
    }

    /**
     * GET /api/common-bets?category=POLITICS
     */
    @GetMapping("/common-bets")
    public ResponseEntity<List<CommonBetDto>> getCommonBets(
            @RequestParam(defaultValue = "POLITICS") Category category) {
        return ResponseEntity.ok(commonBetsService.getCommonBets(category));
    }

    /**
     * GET /api/closed-common-bets?category=POLITICS
     * Son birkac gun icinde sonuclanmis ortak bahisler (kim kazanmis/kaybetmis, kar/zarar).
     */
    @GetMapping("/closed-common-bets")
    public ResponseEntity<List<ClosedCommonBetDto>> getClosedCommonBets(
            @RequestParam(defaultValue = "POLITICS") Category category) {
        return ResponseEntity.ok(closedCommonBetsService.getClosedCommonBets(category));
    }

    /**
     * POST /api/sync?category=POLITICS
     * Zamanlanmis job'u beklemeden manuel yenileme icin (demo/test amacli).
     */
    @PostMapping("/sync")
    public ResponseEntity<String> triggerSync(@RequestParam Category category) {
        syncService.syncCategory(category);
        return ResponseEntity.ok("Kategori senkronize edildi: " + category);
    }
}
