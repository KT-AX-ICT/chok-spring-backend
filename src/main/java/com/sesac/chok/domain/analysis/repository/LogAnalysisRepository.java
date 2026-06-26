package com.sesac.chok.domain.analysis.repository;

import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogAnalysisRepository extends JpaRepository<LogAnalysis, Long> {

    /**
     * 목록 조회("주의 로그 AI 분석"). 2차 이상 판정({@code log.isAbnormal = true})만 반환한다 —
     * 정본은 {@code bgl_log.is_abnormal}이며 정상(false)·미분석(null) 분석은 제외한다.
     * <p>다중 nullable 필터(미지정=null이면 조건 제외): 날짜는 {@code log.occurredAt} 반열린
     * 구간 {@code [startAt, endAt)}, {@code riskLevel}은 단일 정확 일치, keyword는
     * summary·analysis·action OR 부분검색.
     */
    @EntityGraph(attributePaths = "log")
    @Query(value = """
            SELECT a FROM LogAnalysis a
            WHERE a.log.isAbnormal = TRUE
              AND (:startAt   IS NULL OR a.log.occurredAt >= :startAt)
              AND (:endAt     IS NULL OR a.log.occurredAt <  :endAt)
              AND (:riskLevel IS NULL OR a.riskLevel = :riskLevel)
              AND (:keyword   IS NULL
               OR a.summary  LIKE CONCAT('%', :keyword, '%')
               OR a.analysis LIKE CONCAT('%', :keyword, '%')
               OR a.action   LIKE CONCAT('%', :keyword, '%'))
            """,
            countQuery = """
            SELECT COUNT(a) FROM LogAnalysis a
            WHERE a.log.isAbnormal = TRUE
              AND (:startAt   IS NULL OR a.log.occurredAt >= :startAt)
              AND (:endAt     IS NULL OR a.log.occurredAt <  :endAt)
              AND (:riskLevel IS NULL OR a.riskLevel = :riskLevel)
              AND (:keyword   IS NULL
               OR a.summary  LIKE CONCAT('%', :keyword, '%')
               OR a.analysis LIKE CONCAT('%', :keyword, '%')
               OR a.action   LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<LogAnalysis> search(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("riskLevel") String riskLevel,
            @Param("keyword") String keyword,
            Pageable pageable);

    /** 위험도 분포 집계 projection. */
    interface RiskLevelCount {
        String getRiskLevel();

        long getCount();
    }

    /**
     * {@code [startAt, endAt)} 범위(연결된 로그의 occurredAt 기준) 분석 결과를 위험도별로 집계한다.
     * 대시보드 위험도 분포 + 분석 완료 수(합계)에 사용한다.
     */
    @Query("select la.riskLevel as riskLevel, count(la) as count "
            + "from LogAnalysis la "
            + "where la.log.occurredAt >= :startAt and la.log.occurredAt < :endAt "
            + "group by la.riskLevel")
    List<RiskLevelCount> countByRiskLevelInRange(
            @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    /** 주어진 로그들 중 분석 결과가 존재하는 logId 목록 (recentCautionLog.isAnalysis 판정용). */
    @Query("select distinct la.log.id from LogAnalysis la where la.log.id in :logIds")
    List<Long> findAnalyzedLogIds(@Param("logIds") List<Long> logIds);

    /** 반복 패턴 카드 집계 projection. patternId는 {@code pattern_view.id}(=cluster 번호)이다. */
    interface RecentPatternCount {
        Long getPatternId();

        String getPatternName();

        Integer getImportance();

        long getCount();
    }

    /**
     * {@code [startAt, endAt)} 범위(연결된 로그의 occurredAt 기준) 분석 결과를 cluster_id별로 카운트하고
     * {@code pattern_view}와 theta-join해 패턴명·중요도를 함께 돌려준다(대시보드 "반복 탐지 패턴" 카드).
     * cluster_id↔pattern_view.id는 JPA 연관 없이 스칼라 FK라 명시적 등치 join을 쓴다.
     * 미분류 sentinel({@code excludedClusterId}, 보통 99)은 패턴이 아니므로 제외하고, 중요도 내림차순으로 정렬한다.
     */
    @Query("select p.id as patternId, p.patternName as patternName, "
            + "p.importance as importance, count(la) as count "
            + "from PatternView p, LogAnalysis la "
            + "where la.clusterId = p.id "
            + "and p.id <> :excludedClusterId "
            + "and la.log.occurredAt >= :startAt and la.log.occurredAt < :endAt "
            + "group by p.id, p.patternName, p.importance "
            + "order by p.importance desc, count(la) desc")
    List<RecentPatternCount> findRecentPatternsInRange(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("excludedClusterId") long excludedClusterId);

    /** 로그 상세(§3.3): 특정 로그의 분석 결과를 최신순 1건 조회. */
    Optional<LogAnalysis> findFirstByLog_IdOrderByAnalyzedAtDesc(Long logId);

    /** 패턴 상세(§3.6): 특정 패턴에 속하는 분석 건들을 로그 발생 시각 역순으로 반환. */
    @EntityGraph(attributePaths = "log")
    List<LogAnalysis> findByClusterIdOrderByLog_OccurredAtDesc(Long clusterId);

    /** 패턴 목록(§3.5): 패턴별 관련 분석 건수를 집계. clusterId=null(정상 로그)은 제외. */
    @Query("SELECT a.clusterId, COUNT(a) FROM LogAnalysis a WHERE a.clusterId IS NOT NULL GROUP BY a.clusterId")
    List<Object[]> countGroupByClusterId();

    /**
     * 패턴 목록(§3.5): 패턴별 risk_level 분포를 집계한다. clusterId=null(정상 로그)은 제외.
     * 행이 존재하는 (clusterId, riskLevel) 조합만 돌려준다(0건 합성 없음 — 프런트가 누락 단계를 0으로 채움).
     * 반환 형식: {@code [clusterId(Long), riskLevel(String), count(Long)]}.
     */
    @Query("SELECT a.clusterId, a.riskLevel, COUNT(a) FROM LogAnalysis a "
            + "WHERE a.clusterId IS NOT NULL GROUP BY a.clusterId, a.riskLevel")
    List<Object[]> countGroupByClusterIdAndRiskLevel();

    /** 패턴(cluster)별 최고 심각도 projection. severity ordinal: 긴급4·높음3·보통2·낮음1. */
    interface ClusterRiskLevel {
        Long getClusterId();

        int getSeverity();
    }

    /**
     * 패턴 목록·상세(§3.5·§3.6)의 패턴 riskLevel(유저 노출값) 산출용 집계.
     * {@code cluster_id}별로 묶인 분석들의 {@code risk_level} <b>최고 심각도</b>(긴급&gt;높음&gt;보통&gt;낮음)를
     * ordinal로 돌려준다. importance가 아니라 실제 분석 데이터 기준(pattern_view엔 riskLevel 컬럼 없음).
     */
    @Query("SELECT a.clusterId AS clusterId, "
            + "MAX(CASE a.riskLevel WHEN '긴급' THEN 4 WHEN '높음' THEN 3 "
            + "WHEN '보통' THEN 2 WHEN '낮음' THEN 1 ELSE 0 END) AS severity "
            + "FROM LogAnalysis a "
            + "WHERE a.clusterId IS NOT NULL AND a.riskLevel IS NOT NULL "
            + "GROUP BY a.clusterId")
    List<ClusterRiskLevel> findMaxSeverityByCluster();
}
