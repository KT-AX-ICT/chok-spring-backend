package com.sesac.chok.domain.analysis.dto;

import com.sesac.chok.global.type.Domain;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 분석 목록(`GET /analysis`) 응답 항목. `log_analysis` 한 건 + 원시 로그({@code bgl_log}) 요약.
 */
public record AnalysisDto(
        Long analysisId,
        Domain domain,
        String riskLevel,
        String aiSummary,
        String analysis,
        List<String> responsePlan,
        LogInfo log) {

    /**
     * 분석 대상 원시 로그 요약(대시보드 recentCautionLogs와 동일 필드 세트).
     * {@code isCaution}은 {@code label != '-'} 파생값이다.
     */
    public record LogInfo(
            Long logId,
            LocalDateTime occurredAt,
            String node,
            String component,
            String logType,
            String logLevel,
            String label,
            String content,
            boolean isCaution) {
    }
}
