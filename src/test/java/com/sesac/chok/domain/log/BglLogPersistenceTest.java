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
 * <p>2차(FastAPI) 분석 결과를 {@code is_abnormal} Boolean으로 보관한다. 적재 시점엔 {@code null}(미분석),
 * 결과 응답 시 true(이상)/false(정상)로 갱신. 1차 FATAL은 저장하지 않는 파생값이다.
 */
@SpringBootTest
@Transactional
class BglLogPersistenceTest {

    @Autowired
    private BglLogRepository bglLogRepository;

    private static final LocalDateTime TS = LocalDateTime.of(2026, 6, 18, 8, 30, 0);

    @Test
    void isAbnormalIsNullOnInsert() {
        // 적재 시점엔 2차 분석 전이라 is_abnormal은 null(미분석). FATAL 행이어도 마찬가지.
        BglLog saved = bglLogRepository.save(BglLog.builder()
                .occurredAt(TS)
                .node("node-A")
                .logLevel("FATAL")
                .label("KERNDTLB")
                .content("data TLB error interrupt")
                .build());
        bglLogRepository.flush();

        BglLog reloaded = bglLogRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getIsAbnormal()).isNull();
    }

    @Test
    void isAbnormalPersistsVerdictAfterUpdate() {
        // 2차 결과 도착: FATAL이지만 정상 재분류 → false로 갱신·영속.
        BglLog saved = bglLogRepository.save(BglLog.builder()
                .occurredAt(TS)
                .node("node-B")
                .logLevel("FATAL")
                .label("KERNDTLB")
                .content("data TLB error interrupt")
                .build());
        saved.updateAbnormal(false);
        bglLogRepository.flush();

        BglLog reloaded = bglLogRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getIsAbnormal()).isFalse();
    }
}
