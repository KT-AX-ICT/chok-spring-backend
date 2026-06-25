package com.sesac.chok.domain.pattern.dto;

import java.util.List;

/**
 * 패턴 목록(`GET /log-patterns`) 응답 항목.
 * {@code riskLevel}은 {@code importance}(3/2/1) → 한글(높음/보통/낮음) 매핑 파생값.
 * {@code riskCounts}는 패턴별 위험도 분포(대시보드 {@code riskDistribution}과 동일 형태)로,
 * 실제 행이 있는 위험도만 담는다(누락 단계는 프런트가 0으로 채움).
 */
public record PatternSummary(
        Long patternId,
        String patternName,
        String description,
        String representativeLog,
        Integer importance,
        Long count,
        String riskLevel,
        List<RiskCount> riskCounts) {

    /** 패턴별 위험도 분포 항목({@code {riskLevel, count}}). */
    public record RiskCount(String riskLevel, long count) {}
}
