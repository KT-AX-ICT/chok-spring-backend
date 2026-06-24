package com.sesac.chok.domain.pattern.dto;

/**
 * 패턴 목록(`GET /log-patterns`) 응답 항목.
 * {@code riskLevel}은 {@code importance}(3/2/1) → 한글(높음/보통/낮음) 매핑 파생값.
 */
public record PatternSummary(
        Long patternId,
        String patternName,
        String description,
        String representativeLog,
        Integer importance,
        Long count,
        String riskLevel) {}
