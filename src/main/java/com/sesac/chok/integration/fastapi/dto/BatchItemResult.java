package com.sesac.chok.integration.fastapi.dto;

/**
 * 배치 내 개별 항목 결과. 개별 처리 실패가 전체 배치를 막지 않는다(FastAPI 계약).
 * <ul>
 *   <li>{@code processStatus == SUCCESS} → {@code isAbnormal}·{@code result} 존재, {@code error}는 {@code null}</li>
 *   <li>{@code processStatus == FAIL} → {@code error} 존재, {@code isAbnormal}·{@code result}는 {@code null}</li>
 * </ul>
 *
 * @param eventId Tool① 이벤트 템플릿 분류 결과(예: E55). 처리 실패 시 {@code null}
 */
public record BatchItemResult(
        Long logId,
        String eventId,
        ProcessStatus processStatus,
        Boolean isAbnormal,
        AnalyzeResult result,
        String error) {

    /** 처리 성공 항목인지 여부(저장 대상 판별용). */
    public boolean isSuccess() {
        return processStatus == ProcessStatus.SUCCESS;
    }
}
