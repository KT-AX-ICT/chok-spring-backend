package com.sesac.chok.domain.pattern.dto;

import com.sesac.chok.domain.log.dto.LogSummary;
import java.util.List;

/**
 * 패턴 상세(`GET /log-patterns/{patternId}`) 응답.
 * 관련 로그({@code relatedLogs})는 {@code cluster_id}가 이 패턴을 가리키는 분석 건들을 {@code occurredAt,desc}로 반환한다.
 */
public record PatternDetail(
        Long patternId,
        String patternName,
        String description,
        String representativeLog,
        Integer importance,
        String riskLevel,
        List<LogSummary> relatedLogs) {}
