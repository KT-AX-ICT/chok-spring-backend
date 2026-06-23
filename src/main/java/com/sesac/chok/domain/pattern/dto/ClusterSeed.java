package com.sesac.chok.domain.pattern.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * {@code clusters.json}(윤혜림 군집 결과) 한 항목. {@code pattern_view} seed의 입력 형태다.
 * <p>{@code id}는 Python cluster 번호(0..N, 99=미분류)로 {@code pattern_view.id}·
 * {@code log_analysis.cluster_id}와 동일하다(명시 적재). {@code event_template}은 클러스터당 N개라
 * seed에선 대표 1개만 쓴다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClusterSeed(
        long id,
        @JsonProperty("cluster_title") String clusterTitle,
        String description,
        @JsonProperty("event_template") List<EventTemplate> eventTemplate,
        String importance) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventTemplate(@JsonProperty("event_id") String eventId, String template) {
    }

    /** 대표 이벤트 템플릿 = 첫 템플릿. 미분류(템플릿 0개)면 null. */
    public String representativeTemplate() {
        return (eventTemplate == null || eventTemplate.isEmpty()) ? null : eventTemplate.get(0).template();
    }

    /** 정성 importance(High/Middle/Low)를 정렬용 서수(3/2/1)로 매핑. 알 수 없으면 0. */
    public int importanceScore() {
        if (importance == null) {
            return 0;
        }
        return switch (importance.trim().toLowerCase()) {
            case "high" -> 3;
            case "middle" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }
}
