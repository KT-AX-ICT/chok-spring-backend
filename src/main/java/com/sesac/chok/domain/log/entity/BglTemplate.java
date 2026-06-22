package com.sesac.chok.domain.log.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * BGL 이벤트 템플릿 카탈로그 ({@code bgl_template}) — 원본 사전.
 * <p>PK는 자연키 {@code event_id}(예: E1)이며, {@code event_template}은 가변부를 {@code <*>}로 치환한
 * 템플릿 문자열(NOT NULL). 2차 분석(event_id 매칭)은 Python Tool 몫이고, 이 엔티티는 사전의 저장 구조만 담는다.
 */
@Entity
@Table(name = "bgl_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BglTemplate {

    /** 이벤트 ID(예: E1). 자동 생성이 아닌 자연키 — 적재 전 직접 채워야 한다. */
    @Id
    @Column(name = "event_id")
    private String eventId;

    /** 이벤트 템플릿(가변부 {@code <*>}). */
    @Column(name = "event_template", columnDefinition = "TEXT", nullable = false)
    private String eventTemplate;
}
