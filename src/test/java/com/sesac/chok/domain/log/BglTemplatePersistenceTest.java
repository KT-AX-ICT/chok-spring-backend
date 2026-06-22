package com.sesac.chok.domain.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sesac.chok.domain.log.entity.BglTemplate;
import com.sesac.chok.domain.log.repository.BglTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code bgl_template} 저장 구조 검증.
 * <p>BGL 이벤트 템플릿 카탈로그(원본 사전). PK는 자연키 {@code event_id}(예: E1)이며
 * {@code event_template}(가변부 {@code <*>})은 NOT NULL. 2차 분석(event_id 매칭)은 Python Tool 몫이지만,
 * 사전 자체의 저장 구조는 Spring 도메인 모델로 보관한다.
 */
@SpringBootTest
@Transactional
class BglTemplatePersistenceTest {

    @Autowired
    private BglTemplateRepository bglTemplateRepository;

    @Test
    void persistsRoundTripByNaturalKey() {
        bglTemplateRepository.save(BglTemplate.builder()
                .eventId("E1")
                .eventTemplate("data TLB error interrupt <*>")
                .build());
        bglTemplateRepository.flush();

        BglTemplate reloaded = bglTemplateRepository.findById("E1").orElseThrow();
        assertThat(reloaded.getEventId()).isEqualTo("E1");
        assertThat(reloaded.getEventTemplate()).isEqualTo("data TLB error interrupt <*>");
    }

    @Test
    void rejectsNullEventTemplate() {
        // event_template NOT NULL — null 적재는 거부되어야 한다.
        assertThatThrownBy(() -> bglTemplateRepository.saveAndFlush(BglTemplate.builder()
                .eventId("E2")
                .eventTemplate(null)
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
