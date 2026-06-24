package com.sesac.chok.integration.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 분석 결과 본문 (FastAPI 응답 {@code result}, {@code log_analysis} 한 행에 대응).
 * <p>정상·이상 모두 채워지며 정상 로그는 {@code riskLevel}·{@code clusterId}가 {@code null}이고
 * {@code action}이 빈 문자열이다(FastAPI 계약). {@code riskLevel}은 한글 값(긴급/높음/보통/낮음)을
 * 그대로 보존한다(String pass-through).
 *
 * @param analyzedAt 분석/판정 시각. 와이어는 {@code "yyyy-MM-dd HH:mm:ss"} 문자열 → LocalDateTime으로 역직렬화
 */
public record AnalyzeResult(
        String riskLevel,
        String summary,
        String analysis,
        String action,
        Long clusterId,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime analyzedAt) {
}
