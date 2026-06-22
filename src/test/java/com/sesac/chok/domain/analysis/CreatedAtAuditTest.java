package com.sesac.chok.domain.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import com.sesac.chok.global.type.Domain;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code created_at} 적재 시각 자동 채움 검증.
 * <p>적재(write) 경로에서 {@code createdAt}을 수동으로 세팅하지 않아도
 * 영속화 시점에 자동으로 채워져야 한다(NOT NULL 위반 방지).
 */
@SpringBootTest
@Transactional
class CreatedAtAuditTest {

    @Autowired
    private LogAnalysisRepository repository;

    @Autowired
    private BglLogRepository bglLogRepository;

    private static final LocalDateTime TS = LocalDateTime.of(2026, 6, 18, 8, 30, 0);

    @Test
    void bglLogCreatedAtIsAutoPopulatedOnSave() {
        // createdAt 미설정 — 자동 채움이 없으면 NOT NULL 위반으로 저장 실패한다.
        BglLog saved = bglLogRepository.save(BglLog.builder()
                .occurredAt(TS)
                .node("node-A")
                .component("KERNEL")
                .logType("RAS")
                .logLevel("FATAL")
                .label("KERNDTLB")
                .content("data TLB error interrupt")
                .build());

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void logAnalysisCreatedAtIsAutoPopulatedOnSave() {
        BglLog log = bglLogRepository.save(BglLog.builder()
                .occurredAt(TS)
                .node("node-B")
                .label("KERNDTLB")
                .content("err")
                .build());

        // createdAt 미설정 — analyzedAt만 세팅(FastAPI 제공값), createdAt은 적재 시점 자동.
        LogAnalysis saved = repository.save(LogAnalysis.builder()
                .log(log)
                .domain(Domain.BGL)
                .riskLevel("높음")
                .summary("요약")
                .analysis("분석")
                .action("[]")
                .clusterId(99L) // cluster_id NOT NULL (미분류 sentinel)
                .analyzedAt(TS)
                .build());

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
