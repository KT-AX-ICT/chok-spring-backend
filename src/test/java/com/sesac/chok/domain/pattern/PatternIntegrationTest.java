package com.sesac.chok.domain.pattern;

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

    @Test
    void 패턴_목록_count_riskLevel_포함() throws Exception {
        PatternView p1 = patternViewRepository.save(
                PatternView.builder().id(1L).patternName("TLB Error").description("TLB 오류")
                        .eventTemplate("data TLB error").importance(3).build());
        PatternView p2 = patternViewRepository.save(
                PatternView.builder().id(2L).patternName("DDR Error").description("DDR 오류")
                        .eventTemplate("ddr error threshold").importance(2).build());

        BglLog log = bglLogRepository.save(BglLog.builder()
                .occurredAt(TS).node("node-A").component("KERNEL").logType("RAS")
                .logLevel("FATAL").isAbnormal(true).content("data TLB error interrupt").build());

        logAnalysisRepository.save(LogAnalysis.builder()
                .log(log).domain(Domain.BGL).riskLevel("높음").summary("요약").analysis("분석")
                .action("[]").clusterId(p1.getId()).analyzedAt(TS).build());

        mockMvc.perform(get(LIST_URL))
                .andExpect(status().isOk())
                // importance DESC 정렬 → p1(3) 먼저
                .andExpect(jsonPath("$.content[0].patternId").value(1))
                .andExpect(jsonPath("$.content[0].patternName").value("TLB Error"))
                .andExpect(jsonPath("$.content[0].count").value(1))
                .andExpect(jsonPath("$.content[0].riskLevel").value("높음"))
                .andExpect(jsonPath("$.content[1].patternId").value(2))
                .andExpect(jsonPath("$.content[1].count").value(0))
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
                .andExpect(jsonPath("$.relatedLogs").isArray())
                // occurredAt DESC → logLate(09:30) 먼저
                .andExpect(jsonPath("$.relatedLogs[0].node").value("node-B"))
                .andExpect(jsonPath("$.relatedLogs[1].node").value("node-A"))
                .andExpect(jsonPath("$.relatedLogs.length()").value(2));
    }

    @Test
    void 존재하지않는_패턴_404() throws Exception {
        mockMvc.perform(get(DETAIL_URL + "99999"))
                .andExpect(status().isNotFound());
    }
}
