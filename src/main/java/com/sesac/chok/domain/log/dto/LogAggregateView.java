package com.sesac.chok.domain.log.dto;

import java.time.LocalDateTime;

/**
 * 대시보드 집계용 경량 view. 범위 내 로그를 카운트·분포·시간버킷·최근 주의 로그 판정에 필요한 필드만 담아
 * {@code content}(TEXT) 같은 무거운 컬럼의 전 범위 로드를 피한다(스캔 비용 절감).
 * content는 노출 대상인 최근 주의 로그 상위 N건만 {@code findAllById}로 별도 조회한다.
 */
public record LogAggregateView(
        Long id,
        LocalDateTime occurredAt,
        String label,
        String node,
        String component,
        String logType,
        String logLevel) {
}
