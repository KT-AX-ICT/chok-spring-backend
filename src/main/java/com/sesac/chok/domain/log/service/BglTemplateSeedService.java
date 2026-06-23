package com.sesac.chok.domain.log.service;

import com.sesac.chok.domain.log.entity.BglTemplate;
import com.sesac.chok.domain.log.repository.BglTemplateRepository;
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
 * 이벤트 템플릿 카탈로그({@code bgl_template}) 초기 적재. {@code bgl_template}는 정본(SoT, D15)이라
 * Spring이 seed로 초기 1회 채우고, 비어 있을 때만 적재한다(이후 정본은 DB).
 * <p>PK가 자연키 {@code event_id}라 id 채번 이슈가 없어 JPA {@code saveAll}로 직접 적재한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BglTemplateSeedService {

    private static final String SEED_RESOURCE = "seed/BGL_2k.log_templates.csv";

    private final BglTemplateRepository bglTemplateRepository;
    private final BglTemplateCsvParser bglTemplateCsvParser;

    @Transactional
    public void initializeIfEmpty() {
        if (bglTemplateRepository.count() > 0) {
            log.info("bgl_template seed already present. Skip loading.");
            return;
        }
        List<BglTemplate> templates = bglTemplateCsvParser.parse(openSeed());
        bglTemplateRepository.saveAll(templates);
        log.info("bgl_template seed loaded: {} rows.", templates.size());
    }

    private Reader openSeed() {
        try {
            return new InputStreamReader(
                    new ClassPathResource(SEED_RESOURCE).getInputStream(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("bgl_template seed 리소스를 찾을 수 없습니다: " + SEED_RESOURCE, e);
        }
    }
}
