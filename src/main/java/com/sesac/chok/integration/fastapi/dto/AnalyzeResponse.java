package com.sesac.chok.integration.fastapi.dto;

/**
 * 단건 분석 응답 (FastAPI {@code POST /ai/v1/analyze}, 수동/개별 재처리 경로).
 * <p>처리 실패는 에러 응답({@link FastApiErrorResponse})으로 떨어지므로, 이 응답이 오면
 * {@code isAbnormal}·{@code result}는 항상 존재한다(정상 로그는 일부 필드가 빈값).
 *
 * @param eventId Tool① 이벤트 템플릿 분류 결과(예: E55)
 */
public record AnalyzeResponse(
        Long logId,
        String eventId,
        boolean isAbnormal,
        AnalyzeResult result,
        int processingTimeMs) {
}
