package com.sesac.chok.domain.log.service;

import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시연용 BGL seed data 초기 적재를 담당한다.
 * <p>seed data가 없을 때만 classpath의 CSV를 파싱해 적재하고, 이미 있으면 중복 적재하지 않는다.
 * 파싱/저장 책임은 이 서비스에 두며, {@code DataInitializer}는 호출 트리거만 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogSeedService {

    private static final String SEED_RESOURCE = "seed/BGL_2k_chain_scenario_v2.csv";

    private final BglLogRepository bglLogRepository;
    private final BglLogCsvParser bglLogCsvParser;

    @Transactional
    public void initializeIfEmpty() {
        if (bglLogRepository.count() > 0) {
            log.info("BGL seed data already present. Skip loading.");
            return;
        }

        List<BglLog> logs = bglLogCsvParser.parse(openSeed());
        // [학습 메모] BglLog가 GenerationType.IDENTITY라 Hibernate가 JDBC batch insert를 못 하고
        // 여기서 row 수만큼 개별 INSERT가 나간다. 시연용 2,000건은 기동에 1초 미만이라 그대로 둔다.
        // 현업에서 대량 seed/적재라면: ① IDENTITY 대신 SEQUENCE/TABLE 전략 + hibernate.jdbc.batch_size로 배치,
        // ② JDBC bulk insert(또는 DB 네이티브 COPY/LOAD DATA), ③ Flyway/Liquibase 등 마이그레이션 도구로 분리.
        bglLogRepository.saveAll(logs);
        log.info("BGL seed data loaded: {} rows.", logs.size());
    }

    private Reader openSeed() {
        try {
            return new InputStreamReader(
                    new ClassPathResource(SEED_RESOURCE).getInputStream(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("BGL seed 리소스를 찾을 수 없습니다: " + SEED_RESOURCE, e);
        }
    }
}
