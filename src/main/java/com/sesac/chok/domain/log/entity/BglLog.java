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

    /**
     * 2차(FastAPI) 분석 결과. {@code true}=이상, {@code false}=정상.
     * 적재 시점엔 {@code null}(미분석)이고, 2차 분석 결과 응답 시 {@link #updateAbnormal(Boolean)}로 갱신한다.
     * 1차 FATAL 판정({@code log_level == FATAL})은 FastAPI 전송 여부만 정하고 저장하지 않는다(파생값).
     */
    @Column(name = "is_abnormal")
    private Boolean isAbnormal;

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

    /** 2차(FastAPI) 분석 결과로 이상/정상 판정을 채운다(적재 후 결과 도착 시 갱신). */
    public void updateAbnormal(Boolean isAbnormal) {
        this.isAbnormal = isAbnormal;
    }
}
