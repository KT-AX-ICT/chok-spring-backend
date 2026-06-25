package com.sesac.chok.domain.analysis.dto;

import com.sesac.chok.global.type.Domain;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 분석 목록(`GET /analysis`) 응답 항목. `log_analysis` 한 건 + 원시 로그({@code bgl_log}) 요약.
 * {@code clusterId}는 {@code log_analysis.cluster_id}(=pattern_view PK), {@code patternName}은 그 클러스터의
 * 제목이다(정상·미분류 등으로 패턴이 없으면 둘 다 {@code null}/제목 미해소).
 */
public record AnalysisDto(
        Long analysisId,
        Domain domain,
        String riskLevel,
        Long clusterId,
        String patternName,
        String aiSummary,
        String analysis,
        List<String> responsePlan,
        LogInfo log) {

    /**
     * 분석 대상 원시 로그 요약(대시보드 recentCautionLogs와 동일 필드 세트).
     * {@code isCaution}은 시스템 판정 기준 파생값({@link com.sesac.chok.domain.log.entity.BglLog#caution}).
     * {@code label}(답지)은 평가지표 전용이라 응답에 노출하지 않는다.
     */
    public record LogInfo(
            Long logId,
            LocalDateTime occurredAt,
            String node,
            String component,
            String logType,
            String logLevel,
            String content,
            boolean isCaution) {
    }
}
