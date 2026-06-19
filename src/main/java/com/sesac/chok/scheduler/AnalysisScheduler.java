package com.sesac.chok.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnalysisScheduler {

    private static final long FIVE_MINUTES_MS = 300_000L;

    @Scheduled(fixedRate = FIVE_MINUTES_MS)
    public void runAnalysisFlow() {
        try {
            log.info("[Scheduler] analysis scheduler started");

            // TODO: Replace scheduler mock log after LogService.findLogsForAnalysis(limit) is implemented.
            log.info("[Scheduler] pending log target lookup: LogService.findLogsForAnalysis(limit)");

            // TODO: Replace scheduler mock log after FastApiAnalysisClient.analyzeBatch(...) is connected.
            log.info("[Scheduler] pending FastAPI batch analyze call: FastApiAnalysisClient.analyzeBatch(...)");

            // TODO: Replace scheduler mock log after AnalysisService.saveBatchAnalysisResult(...) is implemented.
            log.info("[Scheduler] pending analysis result save: AnalysisService.saveBatchAnalysisResult(...)");

            // TODO: Replace scheduler mock log after PatternService.savePatternResults(...) is implemented.
            log.info("[Scheduler] pending pattern result save: PatternService.savePatternResults(...)");

            log.info("[Scheduler] analysis scheduler completed");
        } catch (Exception e) {
            log.error("[Scheduler] analysis scheduler failed", e);
        }
    }
}
