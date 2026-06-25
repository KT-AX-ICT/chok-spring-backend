package com.sesac.chok.domain.pattern.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sesac.chok.domain.pattern.entity.PatternView;
import com.sesac.chok.domain.pattern.repository.PatternViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code seed/clusters.json}(반복 패턴 9건)을 빈 DB에만 적재하고, 재호출 시 중복하지 않는지 검증한다.
 * <p>핵심: {@code id}가 Python cluster 번호(0·99 포함)로 <b>명시 적재</b>되는지, importance(High/Middle/Low)가
 * 3/2/1로 매핑되는지, 대표 템플릿(첫 템플릿, 미분류는 null)이 들어가는지.
 */
@SpringBootTest
@Transactional
class PatternViewSeedServiceTest {

    private static final int CLUSTER_COUNT = 9;

    @Autowired
    private PatternViewRepository patternViewRepository;

    private PatternViewSeedService seedService;

    @BeforeEach
    void setUp() {
        seedService = new PatternViewSeedService(patternViewRepository);
        patternViewRepository.deleteAllInBatch();
    }

    @Test
    void loadsClustersWithExplicitClusterIds() {
        seedService.initializeIfEmpty();

        assertThat(patternViewRepository.count()).isEqualTo(CLUSTER_COUNT);
        // id가 auto 채번(1..9)이 아니라 cluster 번호(0·99 포함) 명시값이어야 한다.
        assertThat(patternViewRepository.findAllById(java.util.List.of(0L, 99L)))
                .hasSize(2);
        assertThat(patternViewRepository.existsById(0L)).isTrue();
        assertThat(patternViewRepository.existsById(99L)).isTrue();
    }

    @Test
    void mapsTitleImportanceAndRepresentativeTemplate() {
        seedService.initializeIfEmpty();

        PatternView c0 = patternViewRepository.findById(0L).orElseThrow();
        assertThat(c0.getPatternName()).isEqualTo("메모리 인터럽트 오류군");
        assertThat(c0.getImportance()).isEqualTo(3); // High → 3
        assertThat(c0.getEventTemplate()).isEqualTo("data storage interrupt"); // 첫 템플릿
    }

    @Test
    void unclassifiedClusterHasNullTemplate() {
        seedService.initializeIfEmpty();

        PatternView unclassified = patternViewRepository.findById(99L).orElseThrow();
        // 99(미분류)는 event_template 0개 → 대표 템플릿 null.
        assertThat(unclassified.getEventTemplate()).isNull();
    }

    @Test
    void doesNotDuplicateWhenCalledAgain() {
        seedService.initializeIfEmpty();
        seedService.initializeIfEmpty();

        assertThat(patternViewRepository.count()).isEqualTo(CLUSTER_COUNT);
    }
}
