package com.sesac.chok.domain.log.dto;

import com.sesac.chok.global.type.Domain;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 로그 상세(`GET /logs/{logId}`) 응답. 미분석 로그는 {@code analysis}/{@code pattern} null.
 */
public record LogDetailDto(LogInfo log, AnalysisInfo analysis, PatternInfo pattern) {

    public record LogInfo(
            Long logId,
            LocalDateTime occurredAt,
            String node,
            String nodeRepeat,
            String component,
            String logType,
            String logLevel,
            Boolean isAbnormal,
            String eventId,
            boolean isCaution,
            boolean isAnalysis,
            String content) {}

    public record AnalysisInfo(
            Long analysisId,
            Domain domain,
            String riskLevel,
            String aiSummary,
            String analysis,
            List<String> responsePlan) {}

    public record PatternInfo(Long patternId, String patternName, String representativeLog) {}
}
