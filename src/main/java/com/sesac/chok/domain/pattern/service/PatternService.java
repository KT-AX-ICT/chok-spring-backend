package com.sesac.chok.domain.pattern.service;

import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.log.dto.LogSummary;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.pattern.dto.PatternDetail;
import com.sesac.chok.domain.pattern.dto.PatternListResponse;
import com.sesac.chok.domain.pattern.dto.PatternSummary;
import com.sesac.chok.domain.pattern.entity.PatternView;
import com.sesac.chok.domain.pattern.repository.PatternViewRepository;
import com.sesac.chok.global.error.NotFoundException;
import java.util.List;
import java.util.Map;
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

    public PatternListResponse getPatternList(Pageable pageable) {
        Map<Long, Long> countMap = logAnalysisRepository.countGroupByClusterId().stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        Map<Integer, Long> rawImportance = patternViewRepository.countGroupByImportance().stream()
                .collect(Collectors.toMap(r -> (Integer) r[0], r -> (Long) r[1]));
        Map<String, Long> importanceSummary = Map.of(
                "높음", rawImportance.getOrDefault(3, 0L),
                "보통", rawImportance.getOrDefault(2, 0L),
                "낮음", rawImportance.getOrDefault(1, 0L));
        return PatternListResponse.of(
                patternViewRepository.findAll(pageable).map(p -> toSummary(p, countMap.getOrDefault(p.getId(), 0L))),
                importanceSummary);
    }

    public PatternDetail getPatternDetail(Long patternId) {
        PatternView pattern = patternViewRepository.findById(patternId)
                .orElseThrow(() -> NotFoundException.of("pattern_view", patternId));

        List<LogSummary> relatedLogs = logAnalysisRepository
                .findByClusterIdOrderByLog_OccurredAtDesc(patternId)
                .stream()
                .map(a -> toLogSummary(a.getLog(), a.getRiskLevel()))
                .toList();

        return new PatternDetail(pattern.getId(), pattern.getPatternName(), pattern.getDescription(),
                pattern.getEventTemplate(), pattern.getImportance(), relatedLogs);
    }

    private PatternSummary toSummary(PatternView p, Long count) {
        return new PatternSummary(p.getId(), p.getPatternName(), p.getDescription(),
                p.getEventTemplate(), p.getImportance(), count, importanceToRiskLevel(p.getImportance()));
    }

    private LogSummary toLogSummary(BglLog log, String riskLevel) {
        return new LogSummary(log.getId(), log.getOccurredAt(), log.getNode(), log.getComponent(),
                log.getLogType(), log.getLogLevel(), log.getIsAbnormal(), log.getContent(), riskLevel);
    }

    private String importanceToRiskLevel(Integer importance) {
        if (importance == null) return null;
        return switch (importance) {
            case 3 -> "높음";
            case 2 -> "보통";
            case 1 -> "낮음";
            default -> null;
        };
    }
}
