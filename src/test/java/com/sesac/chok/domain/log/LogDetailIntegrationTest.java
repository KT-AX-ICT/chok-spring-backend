package com.sesac.chok.domain.log;

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
 * `GET /logs/{logId}` end-to-end 통합 테스트(§3.3).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class LogDetailIntegrationTest {

    private static final String URL = "/api/v1/logs/";
    private static final LocalDateTime TS = LocalDateTime.of(2026, 6, 18, 8, 30, 0);

    @Autowired MockMvc mockMvc;
    @Autowired BglLogRepository bglLogRepository;
    @Autowired LogAnalysisRepository logAnalysisRepository;
    @Autowired PatternViewRepository patternViewRepository;

    @BeforeEach
    void setUp() {
        logAnalysisRepository.deleteAllInBatch();
        bglLogRepository.deleteAllInBatch();
        patternViewRepository.deleteAllInBatch();
    }

    @Test
    void 분석있는_로그_상세_반환() throws Exception {
        PatternView pattern = patternViewRepository.save(PatternView.builder()
                .id(1L).patternName("TLB Error").description("TLB 오류").eventTemplate("data TLB error")
                .importance(3).build());

        BglLog log = bglLogRepository.save(BglLog.builder()
                .occurredAt(TS).node("node-A").nodeRepeat("node-A").component("KERNEL")
                .logType("RAS").logLevel("FATAL").isAbnormal(true).content("data TLB error interrupt")
                .build());

        logAnalysisRepository.save(LogAnalysis.builder()
                .log(log).domain(Domain.BGL).riskLevel("높음").summary("요약").analysis("분석")
                .action("[\"격리\",\"점검\"]").clusterId(pattern.getId())
                .analyzedAt(TS).build());

        mockMvc.perform(get(URL + log.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.log.logId").value(log.getId()))
                .andExpect(jsonPath("$.log.logLevel").value("FATAL"))
                .andExpect(jsonPath("$.log.isCaution").value(true))
                .andExpect(jsonPath("$.log.isAnalysis").value(true))
                .andExpect(jsonPath("$.analysis.riskLevel").value("높음"))
                .andExpect(jsonPath("$.analysis.responsePlan[0]").value("격리"))
                .andExpect(jsonPath("$.pattern.patternId").value(1))
                .andExpect(jsonPath("$.pattern.patternName").value("TLB Error"))
                .andExpect(jsonPath("$.pattern.representativeLog").value("data TLB error"));
    }

    @Test
    void 미분석_로그_상세_analysis_pattern_null() throws Exception {
        BglLog log = bglLogRepository.save(BglLog.builder()
                .occurredAt(TS).node("node-B").component("MMCS").logType("RAS")
                .logLevel("INFO").isAbnormal(null).content("generating core file").build());

        mockMvc.perform(get(URL + log.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.log.logId").value(log.getId()))
                .andExpect(jsonPath("$.log.isAnalysis").value(false))
                .andExpect(jsonPath("$.analysis").doesNotExist())
                .andExpect(jsonPath("$.pattern").doesNotExist());
    }

    @Test
    void 존재하지않는_로그_404() throws Exception {
        mockMvc.perform(get(URL + "99999"))
                .andExpect(status().isNotFound());
    }
}
