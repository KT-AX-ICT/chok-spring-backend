package com.sesac.chok.domain.log.service;

import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.analysis.service.ResponsePlanParser;
import com.sesac.chok.domain.log.dto.LogDetailDto;
import com.sesac.chok.domain.log.dto.LogSearchCondition;
import com.sesac.chok.domain.log.dto.LogSummary;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import com.sesac.chok.domain.pattern.entity.PatternView;
import com.sesac.chok.domain.pattern.repository.PatternViewRepository;
import com.sesac.chok.global.dto.PageResponse;
import com.sesac.chok.global.error.NotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그 목록·상세 조회. 목록({@code GET /logs})은 다중 nullable 필터 + 페이지네이션을 repository에 위임한다.
 * 상세({@code GET /logs/{logId}})는 {@code bgl_log} 단건 + LEFT JOIN {@code log_analysis}/{@code pattern_view}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogService {

    private final BglLogRepository bglLogRepository;
    private final LogAnalysisRepository logAnalysisRepository;
    private final PatternViewRepository patternViewRepository;

    public PageResponse<LogSummary> getLogs(LogSearchCondition cond, Pageable pageable) {
        return PageResponse.of(bglLogRepository.searchLogs(
                cond.startAt(), cond.endAt(), cond.riskLevel(), cond.logType(), cond.component(),
                cond.logLevel(), cond.keyword(), cond.isCaution(), cond.isAnalysis(),
                pageable));
    }

    public LogDetailDto getLogDetail(Long logId) {
        BglLog log = bglLogRepository.findById(logId)
                .orElseThrow(() -> NotFoundException.of("bgl_log", logId));

        Optional<LogAnalysis> analysisOpt =
                logAnalysisRepository.findFirstByLog_IdOrderByAnalyzedAtDesc(logId);

        LogDetailDto.AnalysisInfo analysisInfo = analysisOpt.map(this::toAnalysisInfo).orElse(null);
        LogDetailDto.PatternInfo patternInfo = analysisOpt
                .map(LogAnalysis::getClusterId)
                .flatMap(patternViewRepository::findById)
                .map(this::toPatternInfo)
                .orElse(null);

        return new LogDetailDto(toLogInfo(log, analysisOpt.isPresent()), analysisInfo, patternInfo);
    }

    private LogDetailDto.LogInfo toLogInfo(BglLog log, boolean isAnalysis) {
        return new LogDetailDto.LogInfo(
                log.getId(), log.getOccurredAt(), log.getNode(), log.getNodeRepeat(),
                log.getComponent(), log.getLogType(), log.getLogLevel(),
                log.getIsAbnormal(), log.getEventId(), log.isCaution(), isAnalysis, log.getContent());
    }

    private LogDetailDto.AnalysisInfo toAnalysisInfo(LogAnalysis a) {
        return new LogDetailDto.AnalysisInfo(
                a.getId(), a.getDomain(), a.getRiskLevel(), a.getSummary(),
                a.getAnalysis(), ResponsePlanParser.parse(a.getAction()));
    }

    private LogDetailDto.PatternInfo toPatternInfo(PatternView p) {
        return new LogDetailDto.PatternInfo(p.getId(), p.getPatternName(), p.getEventTemplate());
    }
}
