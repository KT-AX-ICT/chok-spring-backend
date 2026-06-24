package com.sesac.chok.integration.fastapi.dto;

import java.util.List;

/**
 * 다건 분석 요청 (Spring → FastAPI {@code POST /ai/v1/analyze/batch}). 스케줄러 기본 경로.
 * <p>FastAPI 계약상 {@code logs}는 1건 이상 최대 500건이며, 초과 시 FastAPI가 422로 응답한다
 * (건수 검증의 정본은 FastAPI). 여기서는 계약 형태만 보유한다.
 */
public record BatchAnalyzeRequest(List<AnalyzeRequest> logs) {
}
