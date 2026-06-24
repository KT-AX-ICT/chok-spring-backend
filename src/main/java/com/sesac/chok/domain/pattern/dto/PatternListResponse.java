package com.sesac.chok.domain.pattern.dto;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

/**
 * `GET /log-patterns` 전용 응답. importance 레벨별 패턴 수 집계를 추가로 포함한다.
 */
public record PatternListResponse(
        List<PatternSummary> content,
        Map<String, Long> importanceSummary,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static PatternListResponse of(Page<PatternSummary> page, Map<String, Long> importanceSummary) {
        return new PatternListResponse(
                page.getContent(), importanceSummary,
                page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
