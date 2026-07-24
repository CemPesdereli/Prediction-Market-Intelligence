package com.example.polybets.repository;

import com.example.polybets.domain.Category;
import com.example.polybets.domain.ClosedPositionSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClosedPositionSnapshotRepository extends JpaRepository<ClosedPositionSnapshot, Long> {

    List<ClosedPositionSnapshot> findByCategory(Category category);

    void deleteByCategory(Category category);
}
