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
 * BGL 원시 로그 ({@code bgl_log}). {@code label}은 정확도 검증용 답지(평가지표)로 보존하되 응답에는
 * 노출하지 않는다. 정상/이상 판정은 2차 결과인 {@code isAbnormal}을 따른다.
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

    /**
     * 시스템 판정 기준 주의 여부(프론트 노출용 파생값). 정본 규칙은 여기 한 곳에 둔다.
     * <ul>
     *   <li>2차 이상({@code isAbnormal == true}) → 주의</li>
     *   <li>2차 전({@code isAbnormal == null})이라도 1차 FATAL이면 주의(안전망)</li>
     *   <li>2차 정상({@code isAbnormal == false})이면 FATAL이어도 비주의(2차 우선)</li>
     * </ul>
     */
    public static boolean caution(Boolean isAbnormal, String logLevel) {
        return Boolean.TRUE.equals(isAbnormal) || (isAbnormal == null && "FATAL".equals(logLevel));
    }

    public boolean isCaution() {
        return caution(this.isAbnormal, this.logLevel);
    }
}
