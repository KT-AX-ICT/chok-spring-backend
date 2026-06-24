package com.sesac.chok.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 시작 시 1회 미분석 FATAL 분석 트리거. 조건/가드:
 * <ul>
 *   <li>{@code analysis.run-on-startup=true} (기본 off, 명시 opt-in)</li>
 *   <li>{@code local}/{@code prod} 프로파일에서만 — test/dev 제외(테스트 오염·무의미 실행 방지)</li>
 *   <li>{@link ApplicationReadyEvent}에 반응 — 모든 ApplicationRunner(=seed 적재 {@code DataInitializer})
 *       완료 후 발화하므로 seed 이후 실행이 보장된다</li>
 *   <li>{@code @Async} 비동기 — 부팅을 막지 않음</li>
 *   <li>실패 격리 — 분석 실패가 앱 기동을 막지 않음</li>
 * </ul>
 * 일감(미분석 FATAL)이 없으면 {@link BatchAnalysisService}가 건너뛴다. "미분석만" 처리라 재기동에도 멱등.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "analysis.run-on-startup", havingValue = "true")
@Profile({"local", "prod"})
@RequiredArgsConstructor
public class StartupAnalysisRunner {

    private final BatchAnalysisService batchAnalysisService;
    private final AnalysisJobProperties properties;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void runOnceOnStartup() {
        try {
            log.info("[Startup] 미분석 FATAL 1회 분석 시작 (limit={})", properties.startupLimit());
            batchAnalysisService.runUnanalyzedFatalAnalysis(
                    properties.startupLimit(), properties.chunkSize());
        } catch (Exception e) {
            log.error("[Startup] 1회 분석 실패 — 앱은 계속 실행", e);
        }
    }
}
