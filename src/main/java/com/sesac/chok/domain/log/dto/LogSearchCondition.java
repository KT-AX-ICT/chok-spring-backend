package com.sesac.chok.domain.log.dto;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 로그 목록(`GET /logs`) 다중 nullable 필터(§3.2). 미지정(null) 항목은 조건에서 제외된다.
 * <p>페이지네이션({@code page}/{@code size}/{@code sort})은 {@code Pageable}로 별도 처리한다.
 */
public record LogSearchCondition(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
        String riskLevel,
        String logType,
        String component,
        String logLevel,
        String keyword,
        Boolean isAbnormal,
        Boolean isCaution,
        Boolean isAnalysis) {
}
