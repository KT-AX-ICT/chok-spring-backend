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

    @Column(name = "status")
    private String status;

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

    @Column(name = "log_ts")
    private LocalDateTime logTs;

    @Column(name = "log_level")
    private String logLevel;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
