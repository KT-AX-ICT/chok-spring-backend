package com.sesac.chok.domain.pattern.dto;

/**
 * 패턴 상세의 riskLevel별 분석 건수 1개 항목.
 * 4단계(긴급/높음/보통/낮음) 고정으로, 해당 패턴에 없는 단계는 {@code count = 0}으로 포함된다.
 */
public record RiskLevelCount(String riskLevel, Long count) {}
