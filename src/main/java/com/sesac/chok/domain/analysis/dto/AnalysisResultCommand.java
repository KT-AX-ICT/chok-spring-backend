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
 * @param clusterId  패턴({@code pattern_view}) 번호. 정상 로그는 {@code null}(FastAPI 계약)
 * @param analyzedAt 분석 생성 시각. batch 응답 누락 시 {@code null} → 서비스가 적재 시각으로 fallback
 * @param isAbnormal 2차 이상 판정. {@code true}=이상/{@code false}=정상. 대상 로그({@code bgl_log})의
 *                   {@code is_abnormal}을 갱신한다. 누락 가능성 대비 {@code Boolean}(nullable)
 * @param eventId    2차(Tool ①) 이벤트 템플릿 분류 결과(예: E55). 대상 로그({@code bgl_log})의
 *                   {@code event_id}를 갱신한다. <b>정상 로그는 매칭이 없어 {@code null}</b>(FastAPI 계약).
 *                   정본({@code bgl_template})에 없는 값이면 서비스가 event_id를 비운 채 경고만 남긴다(관대 처리)
 */
public record AnalysisResultCommand(
        Long logId,
        Domain domain,
        String riskLevel,
        String summary,
        String analysis,
        String action,
        Long clusterId,
        LocalDateTime analyzedAt,
        Boolean isAbnormal,
        String eventId) {
}
