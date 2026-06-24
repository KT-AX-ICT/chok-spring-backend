package com.sesac.chok.domain.analysis.repository;

import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogAnalysisRepository extends JpaRepository<LogAnalysis, Long> {

    /** 목록 조회. keyword가 null이면 전체 반환. summary·analysis·action OR 부분검색. */
    @EntityGraph(attributePaths = "log")
    @Query(value = """
            SELECT a FROM LogAnalysis a
            WHERE :keyword IS NULL
               OR a.summary  LIKE CONCAT('%', :keyword, '%')
               OR a.analysis LIKE CONCAT('%', :keyword, '%')
               OR a.action   LIKE CONCAT('%', :keyword, '%')
            """,
            countQuery = """
            SELECT COUNT(a) FROM LogAnalysis a
            WHERE :keyword IS NULL
               OR a.summary  LIKE CONCAT('%', :keyword, '%')
               OR a.analysis LIKE CONCAT('%', :keyword, '%')
               OR a.action   LIKE CONCAT('%', :keyword, '%')
            """)
    Page<LogAnalysis> search(@Param("keyword") String keyword, Pageable pageable);

    /** 로그 상세(§3.3): 특정 로그의 분석 결과를 최신순 1건 조회. */
    Optional<LogAnalysis> findFirstByLog_IdOrderByAnalyzedAtDesc(Long logId);

    /** 패턴 상세(§3.6): 특정 패턴에 속하는 분석 건들을 로그 발생 시각 역순으로 반환. */
    @EntityGraph(attributePaths = "log")
    List<LogAnalysis> findByClusterIdOrderByLog_OccurredAtDesc(Long clusterId);

    /** 패턴 목록(§3.5): 패턴별 관련 분석 건수를 집계. clusterId=null(정상 로그)은 제외. */
    @Query("SELECT a.clusterId, COUNT(a) FROM LogAnalysis a WHERE a.clusterId IS NOT NULL GROUP BY a.clusterId")
    List<Object[]> countGroupByClusterId();
}
