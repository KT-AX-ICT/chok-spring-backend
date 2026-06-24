package com.sesac.chok.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 5분 주기 미분석 FATAL 분석 트리거. {@code analysis.scheduler.enabled=true}일 때만 빈으로 등록된다(기본 off).
 * 핵심 로직은 {@link BatchAnalysisService}에 위임하고, 여기서는 주기 실행과 실패 격리(다음 주기 재시도)만 담당한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "analysis.scheduler.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AnalysisScheduler {

    private final BatchAnalysisService batchAnalysisService;
    private final AnalysisJobProperties properties;

    @Scheduled(fixedRateString = "${analysis.scheduler.fixed-rate-ms:300000}")
    public void runAnalysisFlow() {
        try {
            batchAnalysisService.runUnanalyzedFatalAnalysis(
                    properties.defaultLimit(), properties.chunkSize());
        } catch (Exception e) {
            log.error("[Scheduler] 주기 분석 실패 — 다음 주기 재시도", e);
        }
    }
}
