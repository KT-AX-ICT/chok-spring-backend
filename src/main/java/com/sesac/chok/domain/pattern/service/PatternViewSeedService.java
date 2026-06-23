package com.sesac.chok.domain.pattern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesac.chok.domain.pattern.dto.ClusterSeed;
import com.sesac.chok.domain.pattern.repository.PatternViewRepository;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 반복 패턴({@code pattern_view}) 초기 적재. 윤혜림 군집 결과({@code clusters.json})를 정본으로 채운다.
 * <p><b>명시 id 적재</b>: {@code pattern_view.id}는 Python cluster 번호(0·99 포함)와 일치해야 하는데
 * 엔티티가 {@code @GeneratedValue(IDENTITY)}라 JPA {@code save}로는 id를 지정할 수 없다. 따라서
 * {@link JdbcTemplate} native insert로 명시 id를 적재한다(엔티티 무변경). {@code created_at}은
 * {@code @CreationTimestamp}가 동작하지 않으므로 여기서 직접 채운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatternViewSeedService {

    private static final String SEED_RESOURCE = "seed/clusters.json";
    private static final String INSERT_SQL =
            "INSERT INTO pattern_view (id, pattern_name, description, event_template, importance, created_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?)";
    /** 정적 seed 파일만 역직렬화하므로 Spring 커스터마이즈가 불필요 — 전용 인스턴스를 둔다. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PatternViewRepository patternViewRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void initializeIfEmpty() {
        if (patternViewRepository.count() > 0) {
            log.info("pattern_view seed already present. Skip loading.");
            return;
        }
        List<ClusterSeed> clusters = readClusters();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.batchUpdate(INSERT_SQL, clusters, clusters.size(), (ps, c) -> {
            ps.setLong(1, c.id());
            ps.setString(2, c.clusterTitle());
            ps.setString(3, c.description());
            ps.setString(4, c.representativeTemplate());
            ps.setInt(5, c.importanceScore());
            ps.setTimestamp(6, now);
        });
        log.info("pattern_view seed loaded: {} rows.", clusters.size());
    }

    private List<ClusterSeed> readClusters() {
        try (InputStream in = new ClassPathResource(SEED_RESOURCE).getInputStream()) {
            return OBJECT_MAPPER.readValue(in, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("pattern_view seed 리소스를 읽을 수 없습니다: " + SEED_RESOURCE, e);
        }
    }
}
