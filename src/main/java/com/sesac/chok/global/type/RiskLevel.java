package com.sesac.chok.global.type;

/**
 * 분석 결과 위험도 4단계. {@code log_analysis.risk_level}(VARCHAR)에 enum 이름으로 저장된다.
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
