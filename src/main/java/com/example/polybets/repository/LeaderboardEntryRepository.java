package com.example.polybets.repository;

import com.example.polybets.domain.Category;
import com.example.polybets.domain.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, Long> {

    List<LeaderboardEntry> findByCategoryOrderByRankAsc(Category category);

    void deleteByCategory(Category category);
}
