package com.sesac.chok.domain.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code bgl_log} 매핑 검증.
 * <p>1차 이상탐지(Spring, {@code log_level == FATAL}) 결과를 {@code is_fatal} boolean으로 보관한다.
 * true = 이상(FATAL) → 2차(Python) 분석 대상. 주의 로그({@code isCaution})와는 별개 개념.
 */
@SpringBootTest
@Transactional
class BglLogPersistenceTest {

    @Autowired
    private BglLogRepository bglLogRepository;

    private static final LocalDateTime TS = LocalDateTime.of(2026, 6, 18, 8, 30, 0);

    @Test
    void isFatalPersistsTrue() {
        BglLog saved = bglLogRepository.save(BglLog.builder()
                .occurredAt(TS)
                .node("node-A")
                .logLevel("FATAL")
                .label("KERNDTLB")
                .content("data TLB error interrupt")
                .isFatal(true)
                .build());
        bglLogRepository.flush();

        BglLog reloaded = bglLogRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.isFatal()).isTrue();
    }

    @Test
    void isFatalDefaultsToFalseWhenUnset() {
        BglLog saved = bglLogRepository.save(BglLog.builder()
                .occurredAt(TS)
                .node("node-B")
                .logLevel("INFO")
                .label("-")
                .content("generating core")
                .build());
        bglLogRepository.flush();

        BglLog reloaded = bglLogRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.isFatal()).isFalse();
    }
}
