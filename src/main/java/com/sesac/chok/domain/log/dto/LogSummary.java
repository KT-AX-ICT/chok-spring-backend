package com.sesac.chok.domain.log.dto;

import com.sesac.chok.domain.log.entity.BglLog;
import java.time.LocalDateTime;

/**
 * 로그 목록(`GET /logs`) 응답 항목. {@code bgl_log} 요약 + 분석 여부/위험도({@code log_analysis} LEFT JOIN).
 * <p>{@code isCaution}(시스템 판정 기준 주의)·{@code isAnalysis}({@code log_analysis} 존재)는 파생값이다.
 * {@code label}(답지)은 평가지표 전용이라 응답에 노출하지 않는다.
 */
public record LogSummary(
        Long logId,
        LocalDateTime occurredAt,
        String node,
        String component,
        String logType,
        String logLevel,
        boolean isCaution,
        boolean isAnalysis,
        String content,
        String riskLevel) {

    /**
     * JPQL 생성자 투영용. {@code riskLevel}은 LEFT JOIN된 {@code log_analysis.risk_level}(미분석이면 null).
     * <p>{@code isCaution}은 시스템 판정 기준이다: 2차 결과가 이상({@code isAbnormal=true})이거나,
     * 아직 2차 전({@code isAbnormal=null})이라도 1차 안전망인 FATAL이면 주의로 본다. 2차가 정상
     * ({@code isAbnormal=false})으로 판정하면 FATAL이어도 비주의로 내려간다(2차 우선).
     */
    public LogSummary(
            Long logId, LocalDateTime occurredAt, String node, String component,
            String logType, String logLevel, Boolean isAbnormal, String content, String riskLevel, Long analysisId) {
        this(logId, occurredAt, node, component, logType, logLevel,
                BglLog.caution(isAbnormal, logLevel),
                analysisId != null, content, riskLevel);
    }
}
