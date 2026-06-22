package com.sesac.chok.domain.log.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code seed/BGL_2k_chain_scenario_v2.csv}(시연용 2,000건)를 빈 DB에만 적재하고,
 * 재호출 시 중복 적재하지 않는지 검증한다.
 */
@SpringBootTest
@Transactional
class LogSeedServiceTest {

    private static final int SEED_ROW_COUNT = 2000;

    @Autowired
    private BglLogRepository bglLogRepository;

    private LogSeedService logSeedService;

    @BeforeEach
    void setUp() {
        logSeedService = new LogSeedService(bglLogRepository, new BglLogCsvParser());
    }

    @Test
    void loadsSeedRowsWhenRepositoryIsEmpty() {
        bglLogRepository.deleteAllInBatch();

        logSeedService.initializeIfEmpty();

        assertThat(bglLogRepository.count()).isEqualTo(SEED_ROW_COUNT);
    }

    @Test
    void doesNotDuplicateWhenCalledAgain() {
        bglLogRepository.deleteAllInBatch();

        logSeedService.initializeIfEmpty();
        logSeedService.initializeIfEmpty();

        assertThat(bglLogRepository.count()).isEqualTo(SEED_ROW_COUNT);
    }

    @Test
    void preservesLabelBasedAnomalyRows() {
        bglLogRepository.deleteAllInBatch();

        logSeedService.initializeIfEmpty();

        List<BglLog> abnormal = bglLogRepository.findAll().stream()
                .filter(log -> !"-".equals(log.getLabel()))
                .toList();

        assertThat(abnormal).isNotEmpty();
        assertThat(abnormal).anyMatch(log -> "KERNDTLB".equals(log.getLabel()));
    }
}
