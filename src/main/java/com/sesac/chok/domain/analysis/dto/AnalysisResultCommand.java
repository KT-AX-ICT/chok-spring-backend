package com.sesac.chok.domain.analysis.dto;

import com.sesac.chok.global.type.Domain;
import java.time.LocalDateTime;

/**
 * 분석 결과 적재(write) 입력. FastAPI 응답을 도메인 모델로 변환하기 위한 경계 입력이다.
 * <p>{@code integration.fastapi}의 응답 DTO와 분리해, 외부 계약 변경이 저장 모델로 새지 않게 한다
 * (응답 DTO → 이 command 매핑은 호출 계층의 책임).
 *
 * @param logId      분석 대상 원시 로그({@code bgl_log}) PK
 * @param domain     분석 도메인
 * @param riskLevel  위험도 한글 값(긴급/높음/보통/낮음) — String pass-through
 * @param summary    AI 요약
 * @param analysis   분석 본문
 * @param action     대응 방안(원본 포맷 보존, 조회 시 파싱)
 * @param clusterId  패턴({@code pattern_view}) 번호. 미분류는 99(Python이 채워 보냄) — 항상 존재(NOT NULL)
 * @param analyzedAt 분석 생성 시각. batch 응답 누락 시 {@code null} → 서비스가 적재 시각으로 fallback
 */
public record AnalysisResultCommand(
        Long logId,
        Domain domain,
        String riskLevel,
        String summary,
        String analysis,
        String action,
        long clusterId,
        LocalDateTime analyzedAt) {
}
