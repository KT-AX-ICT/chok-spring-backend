package com.sesac.chok.domain.analysis.service;

import com.sesac.chok.domain.analysis.dto.AnalysisDto;
import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final LogAnalysisRepository logAnalysisRepository;

    public PageResponse<AnalysisDto> getAnalysisList(Pageable pageable) {
        return PageResponse.of(logAnalysisRepository.findAll(pageable).map(this::toDto));
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
                log.getLogTs(),
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
