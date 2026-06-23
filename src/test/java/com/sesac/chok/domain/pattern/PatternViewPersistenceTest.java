package com.sesac.chok.domain.pattern;

import static org.assertj.core.api.Assertions.assertThat;

import com.sesac.chok.domain.pattern.entity.PatternView;
import com.sesac.chok.domain.pattern.repository.PatternViewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code pattern_view} 저장 구조 검증.
 * <p>FastAPI(ChromaDB) 패턴 군집 결과를 Spring 도메인 모델로 저장한다(저장 구조 = 이석진,
 * 군집 기준·조회 API = 윤혜림). {@code event_template} ↔ representativeLog 의미 확정은 보류 —
 * 컬럼 자체는 event_template 고정이라 저장 구조에는 영향 없음.
 */
@SpringBootTest
@Transactional
class PatternViewPersistenceTest {

    @Autowired
    private PatternViewRepository patternViewRepository;

    @Test
    void persistsRoundTrip() {
        PatternView saved = patternViewRepository.save(PatternView.builder()
                .id(12L) // id는 assigned(cluster 번호) — 직접 지정해야 한다
                .patternName("Data TLB Error")
                .description("커널 데이터 TLB 오류 군집")
                .eventTemplate("data TLB error interrupt")
                .importance(90)
                .build());
        patternViewRepository.flush();

        PatternView reloaded = patternViewRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPatternName()).isEqualTo("Data TLB Error");
        assertThat(reloaded.getDescription()).isEqualTo("커널 데이터 TLB 오류 군집");
        assertThat(reloaded.getEventTemplate()).isEqualTo("data TLB error interrupt");
        assertThat(reloaded.getImportance()).isEqualTo(90);
    }

    @Test
    void createdAtIsAutoPopulatedOnSave() {
        // createdAt 미설정 — 자동 채움이 없으면 NOT NULL 위반으로 저장 실패한다.
        PatternView saved = patternViewRepository.save(PatternView.builder()
                .id(13L) // id는 assigned — 직접 지정
                .patternName("Network Timeout")
                .importance(40)
                .build());
        patternViewRepository.flush();

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
