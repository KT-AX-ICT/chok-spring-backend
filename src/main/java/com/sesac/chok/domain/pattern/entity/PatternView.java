package com.sesac.chok.domain.pattern.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 반복 패턴 뷰 ({@code pattern_view}).
 * <p>FastAPI(ChromaDB) 군집 결과를 보관한다. {@code log_analysis.cluster_id}가 이 PK를 참조하며,
 * 미분류 건은 Python이 {@code 99}로 채워 보낸다(연관관계 없이 스칼라 FK로 연결).
 * <p><b>{@code id}는 자동 채번이 아니라 cluster 번호(assigned)</b> — seed·런타임 모두 Python이 부여한
 * cluster 번호(0·99 포함)를 그대로 PK로 쓴다(surrogate가 아니라 의미 있는 키라 IDENTITY 미사용).
 */
@Entity
@Table(name = "pattern_view")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PatternView {

    /** cluster 번호(Python 부여, 0·99 포함). 자동 생성이 아닌 assigned 키 — 적재 전 직접 채워야 한다. */
    @Id
    private Long id;

    /** 클러스터 제목. */
    @Column(name = "pattern_name")
    private String patternName;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 이벤트 템플릿. representativeLog와의 의미 정합은 보류(컬럼 자체는 event_template 고정). */
    @Column(name = "event_template", columnDefinition = "TEXT")
    private String eventTemplate;

    /** 중요도(정렬용). */
    private Integer importance;

    /** DB 적재 시점. 영속화 시점에 Hibernate가 자동 기록한다. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
