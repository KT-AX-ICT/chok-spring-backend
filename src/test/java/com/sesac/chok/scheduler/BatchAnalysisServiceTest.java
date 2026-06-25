package com.sesac.chok.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sesac.chok.domain.analysis.dto.AnalysisResultCommand;
import com.sesac.chok.domain.analysis.service.AnalysisService;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import com.sesac.chok.integration.fastapi.FastApiClient;
import com.sesac.chok.integration.fastapi.dto.AnalyzeResult;
import com.sesac.chok.integration.fastapi.dto.BatchAnalyzeResponse;
import com.sesac.chok.integration.fastapi.dto.BatchItemResult;
import com.sesac.chok.integration.fastapi.dto.ProcessStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BatchAnalysisServiceTest {

    private static final LocalDateTime TS = LocalDateTime.of(2026, 6, 25, 8, 30, 0);

    @Mock BglLogRepository bglLogRepository;
    @Mock FastApiClient fastApiClient;
    @Mock AnalysisService analysisService;

    private BatchAnalysisService service() {
        return new BatchAnalysisService(bglLogRepository, fastApiClient, analysisService);
    }

    @Test
    void 성공항목만_저장하고_실패항목은_건너뛴다() {
        given(bglLogRepository.findUnanalyzedFatal(any())).willReturn(List.of(fatalLog(1L), fatalLog(2L)));
        given(fastApiClient.analyzeBatch(any()))
                .willReturn(response(successItem(1L), failItem(2L)));

        BatchAnalysisResult result = service().runUnanalyzedFatalAnalysis(100, 20);

        assertThat(result).isEqualTo(new BatchAnalysisResult(2, 1, 1));
        verify(analysisService, times(1)).saveAnalysisResult(any(AnalysisResultCommand.class));
    }

    @Test
    void 미분석_FATAL_없으면_FastAPI_호출없이_건너뛴다() {
        given(bglLogRepository.findUnanalyzedFatal(any())).willReturn(List.of());

        BatchAnalysisResult result = service().runUnanalyzedFatalAnalysis(100, 20);

        assertThat(result).isEqualTo(new BatchAnalysisResult(0, 0, 0));
        verifyNoInteractions(fastApiClient);
        verify(analysisService, never()).saveAnalysisResult(any());
    }

    @Test
    void chunk_분석_실패는_격리되어_해당_chunk만_실패로_집계된다() {
        given(bglLogRepository.findUnanalyzedFatal(any())).willReturn(List.of(fatalLog(1L), fatalLog(2L)));
        given(fastApiClient.analyzeBatch(any())).willThrow(new RuntimeException("FastAPI down"));

        BatchAnalysisResult result = service().runUnanalyzedFatalAnalysis(100, 20);

        assertThat(result).isEqualTo(new BatchAnalysisResult(2, 0, 2));
        verify(analysisService, never()).saveAnalysisResult(any());
    }

    @Test
    void chunkSize_단위로_나눠_순차_호출한다() {
        given(bglLogRepository.findUnanalyzedFatal(any()))
                .willReturn(List.of(fatalLog(1L), fatalLog(2L), fatalLog(3L)));
        given(fastApiClient.analyzeBatch(any())).willReturn(response(successItem(1L)));

        BatchAnalysisResult result = service().runUnanalyzedFatalAnalysis(100, 2);

        // 3건 / chunk 2 → 2회 호출
        verify(fastApiClient, times(2)).analyzeBatch(any());
        assertThat(result.requested()).isEqualTo(3);
    }

    @Test
    void 이미_진행_중이면_두번째_트리거는_건너뛴다() {
        BatchAnalysisService svc = service();
        BatchAnalysisResult[] nested = new BatchAnalysisResult[1];
        // 첫 실행이 진행 중(running=true)일 때 재진입한 두 번째 호출은 CAS에서 막혀 건너뛰어야 한다.
        given(bglLogRepository.findUnanalyzedFatal(any())).willAnswer(invocation -> {
            nested[0] = svc.runUnanalyzedFatalAnalysis(100, 20); // 진행 중 재진입
            return List.of();
        });

        svc.runUnanalyzedFatalAnalysis(100, 20);

        assertThat(nested[0]).isEqualTo(new BatchAnalysisResult(0, 0, 0));  // 두 번째 트리거 = 건너뜀
        verify(bglLogRepository, times(1)).findUnanalyzedFatal(any());      // 재진입은 조회까지 안 감(CAS 차단)
        verifyNoInteractions(fastApiClient);
    }

    private static BglLog fatalLog(Long id) {
        return BglLog.builder()
                .id(id).occurredAt(TS).node("R01-M0-N0").nodeRepeat("R01-M0-N0")
                .component("KERNEL").logType("RAS").logLevel("FATAL").content("fatal log " + id)
                .build();
    }

    private static BatchItemResult successItem(Long logId) {
        return new BatchItemResult(logId, "E55", ProcessStatus.SUCCESS, true,
                new AnalyzeResult("높음", "요약", "분석", "[\"격리\"]", 0L, TS), null);
    }

    private static BatchItemResult failItem(Long logId) {
        return new BatchItemResult(logId, null, ProcessStatus.FAIL, null, null, "분석 실패");
    }

    private static BatchAnalyzeResponse response(BatchItemResult... items) {
        return new BatchAnalyzeResponse(items.length, 5, List.of(items));
    }
}
