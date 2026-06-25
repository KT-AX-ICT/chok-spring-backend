package com.sesac.chok.domain.pattern.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesac.chok.domain.pattern.dto.ClusterSeed;
import com.sesac.chok.domain.pattern.entity.PatternView;
import com.sesac.chok.domain.pattern.repository.PatternViewRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
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
    void seedIdSetMatchesClustersJsonOrigin() throws Exception {
        // 원천(clusters.json)의 id 집합을 직접 읽어 seed된 pattern_view의 id 집합과 정확히 일치하는지 단언한다.
        // auto-increment 회귀(1..N → 0·99 누락)나 한쪽만 갱신되는 drift가 생기면 즉시 깨진다.
        List<ClusterSeed> origin = new ObjectMapper().readValue(
                new ClassPathResource("seed/clusters.json").getInputStream(),
                new TypeReference<List<ClusterSeed>>() {});
        Set<Long> expectedIds = origin.stream().map(ClusterSeed::id).collect(Collectors.toSet());

        seedService.initializeIfEmpty();

        Set<Long> actualIds = patternViewRepository.findAll().stream()
                .map(PatternView::getId)
                .collect(Collectors.toSet());
        assertThat(actualIds).isEqualTo(expectedIds);
        // 특히 위험한 경계값 — auto-increment였다면 존재하지 않을 0·99가 반드시 있어야 한다(미분류 FK 대상).
        assertThat(actualIds).contains(0L, 99L);
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
