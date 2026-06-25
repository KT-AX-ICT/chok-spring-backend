package com.sesac.chok.domain.pattern;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import com.sesac.chok.domain.pattern.entity.PatternView;
import com.sesac.chok.domain.pattern.repository.PatternViewRepository;
import com.sesac.chok.global.type.Domain;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * `GET /log-patterns`, `GET /log-patterns/{patternId}` end-to-end 통합 테스트(§3.5·§3.6).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PatternIntegrationTest {

    private static final String LIST_URL = "/api/v1/log-patterns";
    private static final String DETAIL_URL = "/api/v1/log-patterns/";
    private static final LocalDateTime TS = LocalDateTime.of(2026, 6, 18, 8, 30, 0);

    @Autowired MockMvc mockMvc;
    @Autowired PatternViewRepository patternViewRepository;
    @Autowired LogAnalysisRepository logAnalysisRepository;
    @Autowired BglLogRepository bglLogRepository;

    @BeforeEach
    void setUp() {
        logAnalysisRepository.deleteAllInBatch();
        bglLogRepository.deleteAllInBatch();
        patternViewRepository.deleteAllInBatch();
    }

    /** 주어진 패턴(clusterId)에 risk_level 한 건짜리 분석을 적재한다(최고 심각도 집계 검증용). */
    private void saveAnalysis(Long clusterId, String riskLevel) {
        BglLog log = bglLogRepository.save(BglLog.builder()
                .occurredAt(TS).node("node").component("KERNEL").logType("RAS")
                .logLevel("FATAL").isAbnormal(true).content("err").build());
        logAnalysisRepository.save(LogAnalysis.builder()
                .log(log).domain(Domain.BGL).riskLevel(riskLevel).summary("s").analysis("a")
                .action("[]").clusterId(clusterId).analyzedAt(TS).build());
    }

    @Test
    void 패턴_목록_count_와_최고심각도_riskLevel_포함() throws Exception {
        PatternView p1 = patternViewRepository.save(
                PatternView.builder().id(1L).patternName("TLB Error").description("TLB 오류")
                        .eventTemplate("data TLB error").importance(3).build());
        PatternView p2 = patternViewRepository.save(
                PatternView.builder().id(2L).patternName("DDR Error").description("DDR 오류")
                        .eventTemplate("ddr error threshold").importance(2).build());

        // p1: 분석 2건(높음·긴급) → 최고 심각도 = 긴급, count 2
        saveAnalysis(p1.getId(), "높음");
        saveAnalysis(p1.getId(), "긴급");
        // p2: 분석 1건(보통) → 보통, count 1
        saveAnalysis(p2.getId(), "보통");

        mockMvc.perform(get(LIST_URL))
                .andExpect(status().isOk())
                // importance DESC 정렬(내부 기준) → p1(3) 먼저
                .andExpect(jsonPath("$.content[0].patternId").value(1))
                .andExpect(jsonPath("$.content[0].count").value(2))
                .andExpect(jsonPath("$.content[0].riskLevel").value("긴급")) // 높음·긴급 중 최고
                .andExpect(jsonPath("$.content[1].patternId").value(2))
                .andExpect(jsonPath("$.content[1].count").value(1))
                .andExpect(jsonPath("$.content[1].riskLevel").value("보통"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void 패턴_상세_관련로그_역순() throws Exception {
        PatternView pattern = patternViewRepository.save(
                PatternView.builder().id(1L).patternName("TLB Error").description("TLB 오류")
                        .eventTemplate("data TLB error").importance(3).build());

        BglLog logEarly = bglLogRepository.save(BglLog.builder()
                .occurredAt(TS).node("node-A").component("KERNEL").logType("RAS")
                .logLevel("FATAL").isAbnormal(true).content("first error").build());
        BglLog logLate = bglLogRepository.save(BglLog.builder()
                .occurredAt(TS.plusHours(1)).node("node-B").component("KERNEL").logType("RAS")
                .logLevel("FATAL").isAbnormal(true).content("second error").build());

        logAnalysisRepository.save(LogAnalysis.builder()
                .log(logEarly).domain(Domain.BGL).riskLevel("높음").summary("요약").analysis("분석")
                .action("[]").clusterId(pattern.getId()).analyzedAt(TS).build());
        logAnalysisRepository.save(LogAnalysis.builder()
                .log(logLate).domain(Domain.BGL).riskLevel("긴급").summary("요약2").analysis("분석2")
                .action("[]").clusterId(pattern.getId()).analyzedAt(TS.plusHours(1)).build());

        mockMvc.perform(get(DETAIL_URL + pattern.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patternId").value(1))
                .andExpect(jsonPath("$.patternName").value("TLB Error"))
                .andExpect(jsonPath("$.riskLevel").value("긴급")) // 관련 분석 높음·긴급 중 최고 심각도
                .andExpect(jsonPath("$.relatedLogs").isArray())
                // occurredAt DESC → logLate(09:30) 먼저
                .andExpect(jsonPath("$.relatedLogs[0].node").value("node-B"))
                .andExpect(jsonPath("$.relatedLogs[1].node").value("node-A"))
                .andExpect(jsonPath("$.relatedLogs.length()").value(2));
    }

    @Test
    void 패턴_목록_riskLevelSummary_최고심각도별_패턴수() throws Exception {
        PatternView a = patternViewRepository.save(PatternView.builder().id(10L).patternName("A").importance(3).build());
        PatternView b = patternViewRepository.save(PatternView.builder().id(11L).patternName("B").importance(3).build());
        PatternView c = patternViewRepository.save(PatternView.builder().id(12L).patternName("C").importance(2).build());
        // A 최고=긴급, B 최고=긴급(높음·긴급), C 최고=높음 → 긴급 2 / 높음 1
        saveAnalysis(a.getId(), "긴급");
        saveAnalysis(b.getId(), "높음");
        saveAnalysis(b.getId(), "긴급");
        saveAnalysis(c.getId(), "높음");

        mockMvc.perform(get(LIST_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevelSummary.긴급").value(2))
                .andExpect(jsonPath("$.riskLevelSummary.높음").value(1))
                .andExpect(jsonPath("$.riskLevelSummary.보통").value(0))
                .andExpect(jsonPath("$.riskLevelSummary.낮음").value(0));
    }

    @Test
    void 분석없는_패턴은_riskLevel_null() throws Exception {
        patternViewRepository.save(PatternView.builder().id(5L).patternName("Empty").importance(1).build());

        mockMvc.perform(get(LIST_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].count").value(0))
                .andExpect(jsonPath("$.content[0].riskLevel").value(nullValue())); // 분석 없으면 null
    }

    @Test
    void 존재하지않는_패턴_404() throws Exception {
        mockMvc.perform(get(DETAIL_URL + "99999"))
                .andExpect(status().isNotFound());
    }
}
