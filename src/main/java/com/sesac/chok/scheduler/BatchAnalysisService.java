package com.sesac.chok.scheduler;

import com.sesac.chok.domain.analysis.dto.AnalysisResultCommand;
import com.sesac.chok.domain.analysis.service.AnalysisService;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import com.sesac.chok.global.type.Domain;
import com.sesac.chok.integration.fastapi.FastApiClient;
import com.sesac.chok.integration.fastapi.dto.AnalyzeRequest;
import com.sesac.chok.integration.fastapi.dto.AnalyzeResult;
import com.sesac.chok.integration.fastapi.dto.BatchAnalyzeRequest;
import com.sesac.chok.integration.fastapi.dto.BatchAnalyzeResponse;
import com.sesac.chok.integration.fastapi.dto.BatchItemResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 미분석 FATAL 로그를 묶음 단위로 FastAPI 분석 요청 → 결과 저장하는 실행 흐름(여러 서비스 조합 = scheduler 책임).
 * 트리거(5분 스케줄러 / 시작 1회)는 이 {@link #runUnanalyzedFatalAnalysis}를 공유 호출한다.
 *
 * <p>경계: Repository 직접 접근은 대상 조회 1건에 한정하고, 저장은 {@link AnalysisService#saveAnalysisResult}에,
 * FastAPI 호출은 {@link FastApiClient}에 위임한다. 트랜잭션은 저장 1건 단위(부분 진행 허용) — 긴 외부 I/O를
 * 트랜잭션으로 감싸지 않는다.
 *
 * <p>실패 격리: chunk 단위(FastAPI 호출 실패)와 항목 단위(개별 저장 실패) 모두 격리해, 한 건 실패가 나머지 진행을
 * 막지 않는다. 실패는 카운트·로그로 남기고 계속 진행한다.
 *
 * <p>동시 실행 방지(single-flight): 두 트리거(5분 스케줄러·시작 1회)가 겹쳐 같은 미분석 로그를 둘 다 처리하면
 * 분석이 중복 저장된다(read-then-write 레이스). CAS 플래그로 한 번에 하나만 돌게 하고, 진행 중이면 건너뛴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchAnalysisService {

    private final BglLogRepository bglLogRepository;
    private final FastApiClient fastApiClient;
    private final AnalysisService analysisService;

    /** 트리거 동시 실행 방지용 단일 실행 플래그(single-flight). true면 이미 분석이 진행 중. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 미분석 FATAL을 최대 {@code limit}건 조회해 {@code chunkSize}씩 순차로 FastAPI 분석 요청 후 저장한다.
     *
     * @param limit     처리할 미분석 FATAL 상한
     * @param chunkSize FastAPI batch 한 번에 보낼 로그 수(1 미만이면 1로 보정)
     * @return 요청/저장/실패 건수 요약
     */
    public BatchAnalysisResult runUnanalyzedFatalAnalysis(int limit, int chunkSize) {
        // 이미 다른 트리거가 분석 중이면 이번 트리거는 건너뛴다(중복 분석 방지). 남은 일감은 다음 주기에 처리.
        if (!running.compareAndSet(false, true)) {
            log.info("[Batch] 분석이 이미 진행 중 — 이번 트리거는 건너뜀");
            return new BatchAnalysisResult(0, 0, 0);
        }
        try {
            int effectiveChunk = Math.max(chunkSize, 1);
            List<BglLog> targets = bglLogRepository.findUnanalyzedFatal(PageRequest.of(0, Math.max(limit, 0)));
            if (targets.isEmpty()) {
                log.info("[Batch] 미분석 FATAL 없음 — 분석 건너뜀");
                return new BatchAnalysisResult(0, 0, 0);
            }

            log.info("[Batch] 미분석 FATAL {}건 분석 시작 (chunkSize={})", targets.size(), effectiveChunk);
            int saved = 0;
            int failed = 0;
            for (List<BglLog> chunk : partition(targets, effectiveChunk)) {
                try {
                    BatchAnalyzeResponse response = fastApiClient.analyzeBatch(toBatchRequest(chunk));
                    for (BatchItemResult item : response.results()) {
                        if (!item.isSuccess()) {
                            failed++;
                            log.warn("[Batch] 분석 항목 실패 logId={} error={}", item.logId(), item.error());
                            continue;
                        }
                        try {
                            analysisService.saveAnalysisResult(toCommand(item));
                            saved++;
                            log.info("[Batch] 적재 완료 logId={} eventId={} clusterId={} isAbnormal={} riskLevel={}",
                                    item.logId(), item.eventId(),
                                    item.result().clusterId(), item.isAbnormal(), item.result().riskLevel());
                        } catch (Exception e) {
                            failed++;
                            log.warn("[Batch] 결과 저장 실패 logId={} cause={}", item.logId(), e.toString());
                        }
                    }
                } catch (Exception e) {
                    failed += chunk.size();
                    log.error("[Batch] chunk 분석 실패 — 다음 chunk 진행 (chunk={}건)", chunk.size(), e);
                }
            }

            BatchAnalysisResult result = new BatchAnalysisResult(targets.size(), saved, failed);
            log.info("[Batch] 분석 완료: {}", result);
            return result;
        } finally {
            running.set(false);
        }
    }

    private BatchAnalyzeRequest toBatchRequest(List<BglLog> chunk) {
        return new BatchAnalyzeRequest(
                chunk.stream().map(log -> AnalyzeRequest.from(log, Domain.BGL)).toList());
    }

    /** 성공 항목을 저장 입력으로 변환. 성공 항목은 {@code result}가 보장된다(FastAPI 계약). */
    private static AnalysisResultCommand toCommand(BatchItemResult item) {
        AnalyzeResult r = item.result();
        return new AnalysisResultCommand(
                item.logId(), Domain.BGL,
                r.riskLevel(), r.summary(), r.analysis(), r.action(), r.clusterId(),
                r.analyzedAt(), item.isAbnormal(), item.eventId());
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return chunks;
    }
}
