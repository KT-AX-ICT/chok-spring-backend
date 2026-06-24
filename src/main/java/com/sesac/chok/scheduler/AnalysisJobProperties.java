package com.sesac.chok.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 분석 잡 수치 튜너블. 트리거 on/off는 {@code @ConditionalOnProperty}로 별도 게이팅한다
 * ({@code analysis.scheduler.enabled}, {@code analysis.run-on-startup} — 둘 다 기본 off).
 * 여기서는 묶음 크기·1회 처리 상한만 보유한다.
 *
 * @param chunkSize    FastAPI batch 한 번에 보낼 로그 수(LLM 과다호출 방지). 기본 20
 * @param defaultLimit 5분 스케줄러가 1주기에 처리할 미분석 FATAL 상한. 기본 200
 * @param startupLimit 시작 1회 자동 실행이 처리할 미분석 FATAL 상한. 기본 500(시연 데이터 전량 커버)
 */
@ConfigurationProperties(prefix = "analysis")
public record AnalysisJobProperties(
        @DefaultValue("20") int chunkSize,
        @DefaultValue("200") int defaultLimit,
        @DefaultValue("500") int startupLimit) {
}
