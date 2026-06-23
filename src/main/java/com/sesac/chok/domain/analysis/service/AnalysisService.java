package com.sesac.chok.domain.analysis.service;

import com.sesac.chok.domain.analysis.dto.AnalysisDto;
import com.sesac.chok.domain.analysis.dto.AnalysisResultCommand;
import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import com.sesac.chok.global.dto.PageResponse;
import com.sesac.chok.global.error.NotFoundException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final LogAnalysisRepository logAnalysisRepository;
    private final BglLogRepository bglLogRepository;

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
        BglLog log = bglLogRepository.findById(command.logId())
                .orElseThrow(() -> NotFoundException.of("bgl_log", command.logId()));
        log.updateAbnormal(command.isAbnormal()); // 2차 결과 도착 → 대상 로그 이상/정상 판정 갱신

        LogAnalysis saved = logAnalysisRepository.save(LogAnalysis.builder()
                .log(log)
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
                log.getLabel(),
                log.getContent(),
                isCaution(log.getLabel()));
    }

    /** 주의 로그 판정: {@code label}이 alert('-'가 아님)이면 true. (§5) */
    private boolean isCaution(String label) {
        return label != null && !"-".equals(label);
    }
}
