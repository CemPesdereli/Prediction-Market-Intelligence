package com.example.polybets.repository;

import com.example.polybets.domain.Category;
import com.example.polybets.domain.PositionSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionSnapshotRepository extends JpaRepository<PositionSnapshot, Long> {

    List<PositionSnapshot> findByCategory(Category category);

    void deleteByCategory(Category category);
}
