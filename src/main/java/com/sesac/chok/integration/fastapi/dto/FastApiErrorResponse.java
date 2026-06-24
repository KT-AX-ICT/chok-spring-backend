package com.sesac.chok.integration.fastapi.dto;

/**
 * FastAPI 공통 에러 응답(요청 자체 실패 — 422/502/503/500). 배치 <b>개별</b> 항목 실패는
 * 이 스키마가 아니라 {@link BatchItemResult#error()}로 표현된다.
 *
 * @param code    에러 코드(예: {@code VALIDATION_ERROR})
 * @param message 사람용 메시지
 * @param detail  상세(선택)
 */
public record FastApiErrorResponse(String code, String message, String detail) {
}
