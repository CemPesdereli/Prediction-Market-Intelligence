package com.example.polybets.web;

import com.example.polybets.domain.Category;
import com.example.polybets.repository.LeaderboardEntryRepository;
import com.example.polybets.service.ClosedCommonBetsService;
import com.example.polybets.service.CommonBetsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@Controller
public class CommonBetsViewController {

    private final CommonBetsService commonBetsService;
    private final ClosedCommonBetsService closedCommonBetsService;
    private final LeaderboardEntryRepository leaderboardRepository;
    private final String defaultCategory;
    private final int closedWindowDays;

    public CommonBetsViewController(
            CommonBetsService commonBetsService,
            ClosedCommonBetsService closedCommonBetsService,
            LeaderboardEntryRepository leaderboardRepository,
            @Value("${polymarket.default-category}") String defaultCategory,
            @Value("${polymarket.closed-window-days}") int closedWindowDays) {
        this.commonBetsService = commonBetsService;
        this.closedCommonBetsService = closedCommonBetsService;
        this.leaderboardRepository = leaderboardRepository;
        this.defaultCategory = defaultCategory;
        this.closedWindowDays = closedWindowDays;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/common-bets?category=" + defaultCategory;
    }

    @GetMapping("/common-bets")
    public String commonBets(@RequestParam(name = "category", required = false) Category category, Model model) {
        Category selected = category != null ? category : Category.valueOf(defaultCategory);

        model.addAttribute("categories", Category.values());
        model.addAttribute("selectedCategory", selected);
        model.addAttribute("commonBets", commonBetsService.getCommonBets(selected));

        var top20 = leaderboardRepository.findByCategoryOrderByRankAsc(selected);
        model.addAttribute("top20", top20);

        top20.stream()
                .map(e -> e.getSyncedAt())
                .max(Comparator.naturalOrder())
                .ifPresent(instant -> model.addAttribute("lastSyncedAt",
                        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                                .withZone(ZoneId.systemDefault())
                                .format(instant)));

        return "index";
    }

    @GetMapping("/closed-common-bets")
    public String closedCommonBets(@RequestParam(name = "category", required = false) Category category, Model model) {
        Category selected = category != null ? category : Category.valueOf(defaultCategory);

        model.addAttribute("categories", Category.values());
        model.addAttribute("selectedCategory", selected);
        model.addAttribute("closedCommonBets", closedCommonBetsService.getClosedCommonBets(selected));
        model.addAttribute("closedWindowDays", closedWindowDays);

        return "closed-bets";
    }
}
