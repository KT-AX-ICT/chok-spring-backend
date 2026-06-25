package com.sesac.chok.domain.pattern.dto;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

/**
 * `GET /log-patterns` 전용 응답. riskLevel(높음/보통/낮음)별 패턴 수 집계({@code riskLevelSummary})를
 * 추가로 포함한다. (importance는 내부 정렬 기준일 뿐 유저 노출값은 riskLevel.)
 */
public record PatternListResponse(
        List<PatternSummary> content,
        Map<String, Long> riskLevelSummary,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static PatternListResponse of(Page<PatternSummary> page, Map<String, Long> riskLevelSummary) {
        return new PatternListResponse(
                page.getContent(), riskLevelSummary,
                page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
