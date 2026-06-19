package com.sesac.chok.domain.analysis.entity;

import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.global.type.Domain;
import com.sesac.chok.global.type.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 로그 진단/분석 결과 ({@code log_analysis}).
 * <p>원시 로그({@code bgl_log})와 N:1. 패턴 클러스터({@code cluster_id})는 현재 스칼라 FK로만 보관한다.
 */
@Entity
@Table(name = "log_analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LogAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "log_id", nullable = false)
    private BglLog log;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Domain domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String analysis;

    @Column(columnDefinition = "TEXT")
    private String action;

    @Column(name = "cluster_id")
    private Long clusterId;

    /** 분석 생성 시점. */
    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    /** DB 적재 시점. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
