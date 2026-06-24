package com.sesac.chok.scheduler;

/**
 * 분석 잡 1회 실행 결과 요약(로그/반환용).
 *
 * @param requested 조회된 미분석 FATAL 건수
 * @param saved     분석 결과 저장 성공 건수
 * @param failed    FastAPI 항목 실패 + 저장 실패 + chunk 전체 실패 합계
 */
public record BatchAnalysisResult(int requested, int saved, int failed) {
}
