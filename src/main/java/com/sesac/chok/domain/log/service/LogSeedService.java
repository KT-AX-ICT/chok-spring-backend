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
