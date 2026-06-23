package com.sesac.chok.domain.log.dto;

import java.time.LocalDateTime;

/**
 * 로그 목록(`GET /logs`) 응답 항목. {@code bgl_log} 요약 + 분석 여부/위험도({@code log_analysis} LEFT JOIN).
 * <p>{@code isCaution}({@code label != '-'})·{@code isAnalysis}({@code log_analysis} 존재)는 파생값이다.
 */
public record LogSummary(
        Long logId,
        LocalDateTime occurredAt,
        String node,
        String component,
        String logType,
        String logLevel,
        String label,
        boolean isCaution,
        boolean isAnalysis,
        String content,
        String riskLevel) {

    /**
     * JPQL 생성자 투영용. {@code riskLevel}은 LEFT JOIN된 {@code log_analysis.risk_level}(미분석이면 null).
     * {@code isCaution}/{@code isAnalysis}는 여기서 파생 계산한다.
     */
    public LogSummary(
            Long logId, LocalDateTime occurredAt, String node, String component,
            String logType, String logLevel, String label, String content, String riskLevel) {
        this(logId, occurredAt, node, component, logType, logLevel, label,
                label != null && !"-".equals(label), riskLevel != null, content, riskLevel);
    }
}
