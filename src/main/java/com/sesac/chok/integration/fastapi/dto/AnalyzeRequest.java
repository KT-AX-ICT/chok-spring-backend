package com.sesac.chok.integration.fastapi.dto;

import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.global.type.Domain;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 단건 분석 요청 (Spring → FastAPI {@code POST /ai/v1/analyze}, batch 항목 공통).
 * <p>1차 필터(FATAL)를 통과한 로그의 식별자·메타·원문을 <b>통과형(passthrough)</b>으로 전달한다.
 * FastAPI 계약상 {@code label}·{@code eventId}는 보내지 않으며, {@code occurredAt}은 datetime이 아니라
 * {@code "yyyy-MM-dd HH:mm:ss"} 문자열로 넘긴다(FastAPI가 파싱하지 않는 통과형 필드).
 * <p>와이어 포맷은 camelCase(record 필드명 = Jackson 직렬화명).
 */
public record AnalyzeRequest(
        Long logId,
        String node,
        String nodeRepeat,
        String component,
        String logType,
        String occurredAt,
        String logLevel,
        String content,
        String domain) {

    /** FastAPI 통과형 {@code occurredAt} 포맷(KST, Spring 기준). */
    private static final DateTimeFormatter OCCURRED_AT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 원시 로그({@code bgl_log})를 분석 요청으로 변환한다. {@code occurredAt}은 계약 포맷 문자열로 직렬화하고,
     * {@code domain}은 enum 이름(예: {@code "BGL"})으로 넘긴다.
     */
    public static AnalyzeRequest from(BglLog log, Domain domain) {
        LocalDateTime occurredAt = log.getOccurredAt();
        return new AnalyzeRequest(
                log.getId(),
                log.getNode(),
                log.getNodeRepeat(),
                log.getComponent(),
                log.getLogType(),
                occurredAt != null ? occurredAt.format(OCCURRED_AT_FMT) : null,
                log.getLogLevel(),
                log.getContent(),
                domain.name());
    }
}
