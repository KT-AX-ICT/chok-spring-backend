package com.sesac.chok.domain.pattern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesac.chok.domain.pattern.dto.ClusterSeed;
import com.sesac.chok.domain.pattern.entity.PatternView;
import com.sesac.chok.domain.pattern.repository.PatternViewRepository;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 반복 패턴({@code pattern_view}) 초기 적재. 윤혜림 군집 결과({@code clusters.json})를 정본으로 채운다.
 * <p>{@code pattern_view.id}는 Python cluster 번호(0·99 포함)와 일치해야 하는데, 엔티티가 id를
 * <b>assigned</b>로 두므로 cluster 번호를 그대로 {@code id}에 넣어 JPA {@code saveAll}로 적재한다
 * ({@code created_at}은 {@code @CreationTimestamp}가 자동 채움).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatternViewSeedService {

    private static final String SEED_RESOURCE = "seed/clusters.json";
    /** 정적 seed 파일만 역직렬화하므로 Spring 커스터마이즈가 불필요 — 전용 인스턴스를 둔다. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PatternViewRepository patternViewRepository;

    @Transactional
    public void initializeIfEmpty() {
        if (patternViewRepository.count() > 0) {
            log.info("pattern_view seed already present. Skip loading.");
            return;
        }
        List<PatternView> patterns = readClusters().stream()
                .map(c -> PatternView.builder()
                        .id(c.id()) // cluster 번호 = PK(assigned)
                        .patternName(c.clusterTitle())
                        .description(c.description())
                        .eventTemplate(c.representativeTemplate())
                        .importance(c.importanceScore())
                        .build())
                .toList();
        patternViewRepository.saveAll(patterns);
        log.info("pattern_view seed loaded: {} rows.", patterns.size());
    }

    private List<ClusterSeed> readClusters() {
        try (InputStream in = new ClassPathResource(SEED_RESOURCE).getInputStream()) {
            return OBJECT_MAPPER.readValue(in, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("pattern_view seed 리소스를 읽을 수 없습니다: " + SEED_RESOURCE, e);
        }
    }
}
