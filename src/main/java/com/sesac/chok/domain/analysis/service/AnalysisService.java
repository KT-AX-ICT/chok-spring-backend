package com.sesac.chok.domain.analysis.service;

import com.sesac.chok.domain.analysis.dto.AnalysisDto;
import com.sesac.chok.domain.analysis.dto.AnalysisResultCommand;
import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import com.sesac.chok.domain.log.repository.BglTemplateRepository;
import com.sesac.chok.global.dto.PageResponse;
import com.sesac.chok.global.error.NotFoundException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final LogAnalysisRepository logAnalysisRepository;
    private final BglLogRepository bglLogRepository;
    private final BglTemplateRepository bglTemplateRepository;

    public PageResponse<AnalysisDto> getAnalysisList(Pageable pageable) {
        return PageResponse.of(logAnalysisRepository.findAll(pageable).map(this::toDto));
    }

    /**
     * FastAPI 분석 결과를 {@code log_analysis}에 적재한다(저장 책임 = Analysis 도메인).
     * <p>{@code analyzedAt}이 없으면(batch 응답 누락) 적재 시각으로 fallback한다.
     *
     * @return 적재된 분석 결과 PK
     * @throws NotFoundException 대상 로그가 없을 때
     */
    @Transactional
    public Long saveAnalysisResult(AnalysisResultCommand command) {
        BglLog targetLog = bglLogRepository.findById(command.logId())
                .orElseThrow(() -> NotFoundException.of("bgl_log", command.logId()));
        targetLog.updateAbnormal(command.isAbnormal()); // 2차 결과 도착 → 대상 로그 이상/정상 판정 갱신
        applyEventId(targetLog, command.eventId()); // 2차 event_id 반영(정본 검증 + 관대 처리)

        LogAnalysis saved = logAnalysisRepository.save(LogAnalysis.builder()
                .log(targetLog)
                .domain(command.domain())
                .riskLevel(command.riskLevel())
                .summary(command.summary())
                .analysis(command.analysis())
                .action(command.action())
                .clusterId(command.clusterId())
                .analyzedAt(command.analyzedAt() != null ? command.analyzedAt() : LocalDateTime.now())
                .build());

        return saved.getId();
    }

    /**
     * 2차 {@code event_id}를 대상 로그에 반영한다. {@code bgl_template}(정본·SoT)에 없는 값이면
     * <b>적재를 막지 않고</b> {@code event_id}만 비운 채 경고만 남긴다(관대 처리 — 한 건의 미매칭이
     * 분석 적재 전체를 깨뜨리지 않도록). {@code null}(정상 로그는 매칭 없음)이면 그대로 둔다.
     */
    private void applyEventId(BglLog targetLog, String eventId) {
        if (eventId == null) {
            return;
        }
        if (bglTemplateRepository.existsById(eventId)) {
            targetLog.updateEventId(eventId);
        } else {
            log.warn("event_id '{}'가 bgl_template(정본)에 없어 bgl_log.event_id를 비웁니다 (logId={})",
                    eventId, targetLog.getId());
        }
    }

    private AnalysisDto toDto(LogAnalysis entity) {
        return new AnalysisDto(
                entity.getId(),
                entity.getDomain(),
                entity.getRiskLevel(),
                entity.getSummary(),
                entity.getAnalysis(),
                ResponsePlanParser.parse(entity.getAction()),
                toLogInfo(entity.getLog()));
    }

    private AnalysisDto.LogInfo toLogInfo(BglLog log) {
        return new AnalysisDto.LogInfo(
                log.getId(),
                log.getOccurredAt(),
                log.getNode(),
                log.getComponent(),
                log.getLogType(),
                log.getLogLevel(),
                log.getContent(),
                log.isCaution()); // 시스템 판정 기준(2차 이상 또는 2차 전 FATAL)
    }
}
