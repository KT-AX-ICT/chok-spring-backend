package com.sesac.chok.domain.log.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sesac.chok.domain.log.entity.BglTemplate;
import com.sesac.chok.domain.log.repository.BglTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code seed/BGL_2k.log_templates.csv}(이벤트 템플릿 카탈로그 120건)를 빈 DB에만 적재하고,
 * 재호출 시 중복 적재하지 않는지 검증한다. 따옴표 안 콤마 dequote도 함께 확인한다.
 */
@SpringBootTest
@Transactional
class BglTemplateSeedServiceTest {

    private static final int TEMPLATE_COUNT = 120;

    @Autowired
    private BglTemplateRepository bglTemplateRepository;

    private BglTemplateSeedService seedService;

    @BeforeEach
    void setUp() {
        seedService = new BglTemplateSeedService(bglTemplateRepository, new BglTemplateCsvParser());
        bglTemplateRepository.deleteAllInBatch();
    }

    @Test
    void loadsAllTemplatesWhenEmpty() {
        seedService.initializeIfEmpty();

        assertThat(bglTemplateRepository.count()).isEqualTo(TEMPLATE_COUNT);
    }

    @Test
    void doesNotDuplicateWhenCalledAgain() {
        seedService.initializeIfEmpty();
        seedService.initializeIfEmpty();

        assertThat(bglTemplateRepository.count()).isEqualTo(TEMPLATE_COUNT);
    }

    @Test
    void parsesQuotedTemplateWithCommas() {
        seedService.initializeIfEmpty();

        BglTemplate e1 = bglTemplateRepository.findById("E1").orElseThrow();
        // 원본은 따옴표로 quoting되어 내부 콤마를 포함 — dequote 후 콤마가 그대로 보존돼야 한다.
        assertThat(e1.getEventTemplate())
                .isEqualTo("<*> ddr error(s) detected and corrected on rank <*>, symbol <*> over <*> seconds");
    }
}
