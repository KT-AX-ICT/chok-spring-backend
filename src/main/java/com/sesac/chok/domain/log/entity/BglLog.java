package com.sesac.chok.domain.log.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
 * BGL 원시 로그 ({@code bgl_log}). 정상/이상 판정 기준인 {@code label}을 그대로 보존한다.
 */
@Entity
@Table(name = "bgl_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BglLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 1차 이상탐지 결과. {@code log_level == FATAL}이면 true(이상) → 2차(Python) 분석 대상. */
    @Column(name = "is_fatal", nullable = false)
    private boolean isFatal;

    @Column(name = "label")
    private String label;

    @Column(name = "node")
    private String node;

    @Column(name = "node_repeat")
    private String nodeRepeat;

    @Column(name = "component")
    private String component;

    @Column(name = "log_type")
    private String logType;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "log_level")
    private String logLevel;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "event_id")
    private String eventId;

    /** DB 적재 시점. 영속화 시점에 Hibernate가 자동 기록한다. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
