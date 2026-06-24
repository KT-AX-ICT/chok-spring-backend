package com.sesac.chok.integration.fastapi.dto;

import java.util.List;

/**
 * 다건 분석 응답 (FastAPI {@code POST /ai/v1/analyze/batch}).
 *
 * @param totalCount       요청 로그 건수
 * @param processingTimeMs FastAPI 처리 소요(ms)
 * @param results          항목별 결과(성공/실패 혼재)
 */
public record BatchAnalyzeResponse(
        int totalCount,
        int processingTimeMs,
        List<BatchItemResult> results) {
}
