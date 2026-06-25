package com.sesac.chok.domain.analysis.dto;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 분석 목록(`GET /analysis`) 다중 nullable 필터. 미지정(null) 항목은 조건에서 제외된다.
 * <p>날짜는 연결된 로그의 {@code occurred_at} 기준 반열린 구간 {@code [startAt, endAt)},
 * {@code riskLevel}은 단일 정확 일치(매핑 없이 한글값 그대로), {@code keyword}는 부분검색,
 * {@code clusterId}는 패턴({@code cluster_id}) 단일 정확 일치다.
 * 페이지네이션({@code page}/{@code size}/{@code sort})은 {@code Pageable}로 별도 처리한다.
 */
public record AnalysisSearchCondition(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
        String riskLevel,
        String keyword,
        Long clusterId) {
}
