package com.sesac.chok.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 5분 주기 미분석 FATAL 분석 트리거. {@code local}/{@code prod}에서 <b>기본 ON</b>이며
 * ({@code analysis.scheduler.enabled} 미설정 시 동작), {@code analysis.scheduler.enabled=false}로 끈다.
 * test/dev에선 {@code @Profile}로 자동 비활성(테스트 오염·in-memory 무의미 실행 방지).
 * <p>첫 실행은 {@code analysis.scheduler.initial-delay-ms}(기본 15s) 뒤로 미뤄 부팅 직후 seed 적재와의 경합을 피한다.
 * 핵심 로직은 {@link BatchAnalysisService}에 위임하고, 여기서는 주기 실행과 실패 격리(다음 주기 재시도)만 담당한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "analysis.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@Profile({"local", "prod"})
@RequiredArgsConstructor
public class AnalysisScheduler {

    private final BatchAnalysisService batchAnalysisService;
    private final AnalysisJobProperties properties;

    @Scheduled(
            fixedRateString = "${analysis.scheduler.fixed-rate-ms:300000}",
            initialDelayString = "${analysis.scheduler.initial-delay-ms:15000}")
    public void runAnalysisFlow() {
        try {
            batchAnalysisService.runUnanalyzedFatalAnalysis(
                    properties.defaultLimit(), properties.chunkSize());
        } catch (Exception e) {
            log.error("[Scheduler] 주기 분석 실패 — 다음 주기 재시도", e);
        }
    }
}
