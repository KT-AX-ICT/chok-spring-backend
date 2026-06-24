package com.sesac.chok.domain.pattern.repository;

import com.sesac.chok.domain.pattern.entity.PatternView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PatternViewRepository extends JpaRepository<PatternView, Long> {

    @Query("SELECT p.importance, COUNT(p) FROM PatternView p GROUP BY p.importance")
    List<Object[]> countGroupByImportance();
}
