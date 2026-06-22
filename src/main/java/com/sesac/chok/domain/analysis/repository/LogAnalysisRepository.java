package com.sesac.chok.domain.analysis.repository;

import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAnalysisRepository extends JpaRepository<LogAnalysis, Long> {

    /** 원시 로그({@code log})를 함께 로딩해 목록 매핑 시 N+1을 피한다. */
    @Override
    @EntityGraph(attributePaths = "log")
    Page<LogAnalysis> findAll(Pageable pageable);
}
