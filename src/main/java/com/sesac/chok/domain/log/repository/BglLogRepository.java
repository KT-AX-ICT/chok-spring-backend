package com.sesac.chok.domain.log.repository;

import com.sesac.chok.domain.log.dto.LogAggregateView;
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
     * 분석 잡 대상: 아직 분석되지 않은 FATAL 로그. ({@code log_level='FATAL'} AND {@code log_analysis} 없음)
     * <p>시간 조건 없음(시연 데이터 전량 대상). 발생 시각 오름차순(오래된 것 우선)으로 {@code Pageable} limit만큼 반환한다.
     */
    @Query("""
            SELECT b FROM BglLog b
            WHERE b.logLevel = 'FATAL'
              AND NOT EXISTS (SELECT 1 FROM LogAnalysis a WHERE a.log = b)
            ORDER BY b.occurredAt ASC
            """)
    List<BglLog> findUnanalyzedFatal(Pageable pageable);

    /**
     * 로그 목록 조회(§3.2). {@code bgl_log}를 주체로 {@code log_analysis}를 LEFT JOIN해
     * 위험도({@code riskLevel})·분석 여부({@code isAnalysis})를 함께 내려준다.
     * <p>각 필터는 null이면 조건에서 제외되는 {@code (:param IS NULL OR ...)} 패턴이다.
     * {@code keyword}는 {@code content} 부분일치다. {@code isCaution}(주의)은 시스템 판정 기준 —
     * 2차 이상({@code isAbnormal=TRUE}) 또는 2차 전 FATAL({@code isAbnormal IS NULL AND logLevel='FATAL'}).
     * {@code label}(답지)은 평가지표 전용이라 응답·필터 어디에도 노출하지 않는다(컬럼으로만 보존).
     */
    @Query(value = """
            SELECT DISTINCT new com.sesac.chok.domain.log.dto.LogSummary(
                b.id, b.occurredAt, b.node, b.component, b.logType, b.logLevel, b.isAbnormal, b.content, a.riskLevel, a.id)
            FROM BglLog b LEFT JOIN LogAnalysis a ON a.log = b
            WHERE (:startAt IS NULL OR b.occurredAt >= :startAt)
              AND (:endAt IS NULL OR b.occurredAt <= :endAt)
              AND (:riskLevel IS NULL OR a.riskLevel = :riskLevel)
              AND (:logType IS NULL OR b.logType = :logType)
              AND (:component IS NULL OR b.component = :component)
              AND (:logLevel IS NULL OR b.logLevel = :logLevel)
              AND (:keyword IS NULL OR b.content LIKE CONCAT('%', :keyword, '%'))
              AND (:isAbnormal IS NULL OR b.isAbnormal = :isAbnormal)
              AND (:isCaution IS NULL
                   OR (:isCaution = TRUE AND (b.isAbnormal = TRUE OR (b.isAbnormal IS NULL AND b.logLevel = 'FATAL')))
                   OR (:isCaution = FALSE AND (b.isAbnormal = FALSE OR (b.isAbnormal IS NULL AND b.logLevel <> 'FATAL'))))
              AND (:isAnalysis IS NULL
                   OR (:isAnalysis = TRUE AND a.id IS NOT NULL)
                   OR (:isAnalysis = FALSE AND a.id IS NULL))
            """,
            countQuery = """
            SELECT COUNT(DISTINCT b) FROM BglLog b LEFT JOIN LogAnalysis a ON a.log = b
            WHERE (:startAt IS NULL OR b.occurredAt >= :startAt)
              AND (:endAt IS NULL OR b.occurredAt <= :endAt)
              AND (:riskLevel IS NULL OR a.riskLevel = :riskLevel)
              AND (:logType IS NULL OR b.logType = :logType)
              AND (:component IS NULL OR b.component = :component)
              AND (:logLevel IS NULL OR b.logLevel = :logLevel)
              AND (:keyword IS NULL OR b.content LIKE CONCAT('%', :keyword, '%'))
              AND (:isAbnormal IS NULL OR b.isAbnormal = :isAbnormal)
              AND (:isCaution IS NULL
                   OR (:isCaution = TRUE AND (b.isAbnormal = TRUE OR (b.isAbnormal IS NULL AND b.logLevel = 'FATAL')))
                   OR (:isCaution = FALSE AND (b.isAbnormal = FALSE OR (b.isAbnormal IS NULL AND b.logLevel <> 'FATAL'))))
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
            @Param("keyword") String keyword,
            @Param("isAbnormal") Boolean isAbnormal,
            @Param("isCaution") Boolean isCaution,
            @Param("isAnalysis") Boolean isAnalysis,
            Pageable pageable);

    /**
     * {@code [startAt, endAt)} 범위의 로그를 집계용 경량 view로 조회한다. content(TEXT) 등 무거운 컬럼을
     * 제외해 전 범위 스캔 비용을 줄인다. 대시보드 집계의 단일 입력 소스다.
     * <p>정렬은 생략한다 — 집계(시간버킷·분포·카운트)는 순서에 의존하지 않고, 최근 주의 로그는 서비스에서
     * 다시 정렬하므로 SQL 정렬 단계가 불필요하다.
     */
    @Query("SELECT new com.sesac.chok.domain.log.dto.LogAggregateView("
            + "b.id, b.occurredAt, b.isAbnormal, b.node, b.component, b.logType, b.logLevel) "
            + "FROM BglLog b "
            + "WHERE b.occurredAt >= :startAt AND b.occurredAt < :endAt")
    List<LogAggregateView> findAggregateViewInRange(
            @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);
}
