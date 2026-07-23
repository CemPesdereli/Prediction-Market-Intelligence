package com.example.polybets.web;

import com.example.polybets.domain.Category;
import com.example.polybets.repository.LeaderboardEntryRepository;
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
    private final LeaderboardEntryRepository leaderboardRepository;
    private final String defaultCategory;

    public CommonBetsViewController(
            CommonBetsService commonBetsService,
            LeaderboardEntryRepository leaderboardRepository,
            @Value("${polymarket.default-category}") String defaultCategory) {
        this.commonBetsService = commonBetsService;
        this.leaderboardRepository = leaderboardRepository;
        this.defaultCategory = defaultCategory;
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
}
