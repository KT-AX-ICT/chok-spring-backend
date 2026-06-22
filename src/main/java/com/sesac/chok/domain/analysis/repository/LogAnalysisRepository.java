package com.sesac.chok.domain.analysis.repository;

import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogAnalysisRepository extends JpaRepository<LogAnalysis, Long> {

    /** 원시 로그({@code log})를 함께 로딩해 목록 매핑 시 N+1을 피한다. */
    @Override
    @EntityGraph(attributePaths = "log")
    Page<LogAnalysis> findAll(Pageable pageable);

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
}
