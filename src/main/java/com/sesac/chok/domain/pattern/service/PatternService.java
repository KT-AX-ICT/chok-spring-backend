package com.sesac.chok.domain.pattern.service;

import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.log.dto.LogSummary;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.pattern.dto.PatternDetail;
import com.sesac.chok.domain.pattern.dto.PatternListResponse;
import com.sesac.chok.domain.pattern.dto.PatternSummary;
import com.sesac.chok.domain.pattern.entity.PatternView;
import com.sesac.chok.domain.pattern.repository.PatternViewRepository;
import com.sesac.chok.global.error.NotFoundException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatternService {

    private final PatternViewRepository patternViewRepository;
    private final LogAnalysisRepository logAnalysisRepository;

    /** risk_level 심각도 내림차순(가장 심각한 게 index 0). ordinal = size - index → 긴급4·높음3·보통2·낮음1. */
    private static final List<String> SEVERITY_DESC = List.of("긴급", "높음", "보통", "낮음");

    public PatternListResponse getPatternList(Pageable pageable) {
        Map<Long, Long> countMap = logAnalysisRepository.countGroupByClusterId().stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        // 패턴 riskLevel(유저 노출값) = 그 패턴 분석들의 risk_level 최고 심각도(실데이터). importance는 정렬 내부용.
        Map<Long, String> riskLevelByCluster = logAnalysisRepository.findMaxSeverityByCluster().stream()
                .filter(r -> r.getSeverity() >= 1)
                .collect(Collectors.toMap(r -> r.getClusterId(), r -> riskLevelOf(r.getSeverity())));
        Map<String, Long> riskLevelSummary = summarizeRiskLevels(riskLevelByCluster.values());
        return PatternListResponse.of(
                patternViewRepository.findAll(pageable)
                        .map(p -> toSummary(p, countMap.getOrDefault(p.getId(), 0L), riskLevelByCluster.get(p.getId()))),
                riskLevelSummary);
    }

    public PatternDetail getPatternDetail(Long patternId) {
        PatternView pattern = patternViewRepository.findById(patternId)
                .orElseThrow(() -> NotFoundException.of("pattern_view", patternId));

        List<LogAnalysis> analyses = logAnalysisRepository.findByClusterIdOrderByLog_OccurredAtDesc(patternId);
        String riskLevel = maxSeverityRiskLevel(analyses);
        List<LogSummary> relatedLogs = analyses.stream()
                .map(a -> toLogSummary(a.getLog(), a.getRiskLevel()))
                .toList();

        return new PatternDetail(pattern.getId(), pattern.getPatternName(), pattern.getDescription(),
                pattern.getEventTemplate(), pattern.getImportance(), riskLevel, relatedLogs);
    }

    private PatternSummary toSummary(PatternView p, Long count, String riskLevel) {
        return new PatternSummary(p.getId(), p.getPatternName(), p.getDescription(),
                p.getEventTemplate(), p.getImportance(), count, riskLevel);
    }

    private LogSummary toLogSummary(BglLog log, String riskLevel) {
        return new LogSummary(log.getId(), log.getOccurredAt(), log.getNode(), log.getComponent(),
                log.getLogType(), log.getLogLevel(), log.getIsAbnormal(), log.getContent(), riskLevel,
                riskLevel != null ? 1L : null);
    }

    /** risk_level → 심각도 ordinal(긴급4·높음3·보통2·낮음1, 미상/null=0). */
    private int severityOf(String riskLevel) {
        int idx = SEVERITY_DESC.indexOf(riskLevel);
        return idx < 0 ? 0 : SEVERITY_DESC.size() - idx;
    }

    /** 심각도 ordinal → risk_level 한글값(1..4만 유효, 그 외 null). */
    private String riskLevelOf(int severity) {
        return (severity >= 1 && severity <= 4) ? SEVERITY_DESC.get(SEVERITY_DESC.size() - severity) : null;
    }

    /** 분석들의 risk_level 중 최고 심각도 한 건(없으면 null). */
    private String maxSeverityRiskLevel(List<LogAnalysis> analyses) {
        return analyses.stream()
                .map(LogAnalysis::getRiskLevel)
                .filter(Objects::nonNull)
                .filter(rl -> severityOf(rl) >= 1)
                .max(Comparator.comparingInt(this::severityOf))
                .orElse(null);
    }

    /** 패턴 riskLevel 값들을 4단계(긴급/높음/보통/낮음)별 패턴 수로 집계(각 키 0 기본, null 제외). */
    private Map<String, Long> summarizeRiskLevels(Collection<String> riskLevels) {
        Map<String, Long> counts = riskLevels.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(rl -> rl, Collectors.counting()));
        Map<String, Long> summary = new LinkedHashMap<>();
        SEVERITY_DESC.forEach(rl -> summary.put(rl, counts.getOrDefault(rl, 0L)));
        return summary;
    }
}
