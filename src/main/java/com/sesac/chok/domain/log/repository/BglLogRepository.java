package com.sesac.chok.domain.log.repository;

import com.sesac.chok.domain.log.dto.LogSummary;
import com.sesac.chok.domain.log.entity.BglLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BglLogRepository extends JpaRepository<BglLog, Long> {

    /**
     * 로그 목록 조회(§3.2). {@code bgl_log}를 주체로 {@code log_analysis}를 LEFT JOIN해
     * 위험도({@code riskLevel})·분석 여부({@code isAnalysis})를 함께 내려준다.
     * <p>각 필터는 null이면 조건에서 제외되는 {@code (:param IS NULL OR ...)} 패턴이다.
     * {@code keyword}는 {@code content} 부분일치, {@code isCaution}은 {@code label != '-'} 기준이다.
     */
    @Query(value = """
            SELECT new com.sesac.chok.domain.log.dto.LogSummary(
                b.id, b.occurredAt, b.node, b.component, b.logType, b.logLevel, b.label, b.content, a.riskLevel)
            FROM BglLog b LEFT JOIN LogAnalysis a ON a.log = b
            WHERE (:startAt IS NULL OR b.occurredAt >= :startAt)
              AND (:endAt IS NULL OR b.occurredAt <= :endAt)
              AND (:riskLevel IS NULL OR a.riskLevel = :riskLevel)
              AND (:logType IS NULL OR b.logType = :logType)
              AND (:component IS NULL OR b.component = :component)
              AND (:logLevel IS NULL OR b.logLevel = :logLevel)
              AND (:label IS NULL OR b.label = :label)
              AND (:keyword IS NULL OR b.content LIKE CONCAT('%', :keyword, '%'))
              AND (:isCaution IS NULL
                   OR (:isCaution = TRUE AND b.label <> '-')
                   OR (:isCaution = FALSE AND (b.label = '-' OR b.label IS NULL)))
              AND (:isAnalysis IS NULL
                   OR (:isAnalysis = TRUE AND a.id IS NOT NULL)
                   OR (:isAnalysis = FALSE AND a.id IS NULL))
            """,
            countQuery = """
            SELECT COUNT(b) FROM BglLog b LEFT JOIN LogAnalysis a ON a.log = b
            WHERE (:startAt IS NULL OR b.occurredAt >= :startAt)
              AND (:endAt IS NULL OR b.occurredAt <= :endAt)
              AND (:riskLevel IS NULL OR a.riskLevel = :riskLevel)
              AND (:logType IS NULL OR b.logType = :logType)
              AND (:component IS NULL OR b.component = :component)
              AND (:logLevel IS NULL OR b.logLevel = :logLevel)
              AND (:label IS NULL OR b.label = :label)
              AND (:keyword IS NULL OR b.content LIKE CONCAT('%', :keyword, '%'))
              AND (:isCaution IS NULL
                   OR (:isCaution = TRUE AND b.label <> '-')
                   OR (:isCaution = FALSE AND (b.label = '-' OR b.label IS NULL)))
              AND (:isAnalysis IS NULL
                   OR (:isAnalysis = TRUE AND a.id IS NOT NULL)
                   OR (:isAnalysis = FALSE AND a.id IS NULL))
            """)
    Page<LogSummary> searchLogs(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("riskLevel") String riskLevel,
            @Param("logType") String logType,
            @Param("component") String component,
            @Param("logLevel") String logLevel,
            @Param("label") String label,
            @Param("keyword") String keyword,
            @Param("isCaution") Boolean isCaution,
            @Param("isAnalysis") Boolean isAnalysis,
            Pageable pageable);

    /**
     * {@code [startAt, endAt)} 범위의 로그를 시각 오름차순으로 조회한다.
     * 대시보드 집계(시간대 버킷팅, 분포, 최근 주의 로그)의 단일 입력 소스다.
     */
    List<BglLog> findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(
            LocalDateTime startAt, LocalDateTime endAt);
}
