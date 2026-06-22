package com.sesac.chok.domain.dashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isIn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.dashboard.service.DashboardService;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import com.sesac.chok.global.config.CorsConfig;
import com.sesac.chok.global.config.SecurityConfig;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(DashboardController.class)
@Import({DashboardService.class, SecurityConfig.class, CorsConfig.class})
class DashboardControllerTest {

    private static final String DASHBOARD_SUMMARY_URL = "/api/v1/dashboard/summary";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BglLogRepository bglLogRepository;

    @MockitoBean
    private LogAnalysisRepository logAnalysisRepository;

    @Test
    void getDashboardSummaryReturnsOk() throws Exception {
        mockMvc.perform(get(DASHBOARD_SUMMARY_URL))
                .andExpect(status().isOk());
    }

    @Test
    void getDashboardSummaryDefaultsToTwentyFourHourRange() throws Exception {
        MvcResult result = mockMvc.perform(get(DASHBOARD_SUMMARY_URL))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode range = objectMapper.readTree(result.getResponse().getContentAsString()).path("range");
        LocalDateTime startAt = LocalDateTime.parse(range.path("startAt").asText());
        LocalDateTime endAt = LocalDateTime.parse(range.path("endAt").asText());

        assertThat(Duration.between(startAt, endAt)).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void getDashboardSummaryAcceptsStartAtEndAtAndIntervalParameters() throws Exception {
        mockMvc.perform(get(DASHBOARD_SUMMARY_URL)
                        .param("startAt", "2026-06-19T09:00:00")
                        .param("endAt", "2026-06-19T21:00:00")
                        .param("interval", "6h"))
                .andExpect(status().isOk());
    }

    @Test
    void getDashboardSummaryReturnsKoreanRiskLevelEnum() throws Exception {
        mockMvc.perform(get(DASHBOARD_SUMMARY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskDistribution[0].riskLevel",
                        isIn(new String[]{"긴급", "높음", "보통", "낮음"})));
    }

    @Test
    void getDashboardSummaryReturnsTopLevelFields() throws Exception {
        mockMvc.perform(get(DASHBOARD_SUMMARY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range").exists())
                .andExpect(jsonPath("$.stats").exists())
                .andExpect(jsonPath("$.timeSeries").exists())
                .andExpect(jsonPath("$.riskDistribution").exists())
                .andExpect(jsonPath("$.typeDistribution").exists())
                .andExpect(jsonPath("$.componentDistribution").exists())
                .andExpect(jsonPath("$.levelDistribution").exists())
                .andExpect(jsonPath("$.recentCautionLogs").exists())
                .andExpect(jsonPath("$.recentPatterns").exists());
    }

    @Test
    void getDashboardSummaryAggregatesBglLogRowsByBucketAndLabel() throws Exception {
        // 09:00~21:00 / 6h → 버킷 2개: [09,15), [15,21)
        given(bglLogRepository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(any(), any()))
                .willReturn(List.of(
                        row(1L, "2026-06-19T10:00:00", "-", "INFO", false),
                        row(2L, "2026-06-19T11:00:00", "KERNDTLB", "FATAL", true),
                        row(3L, "2026-06-19T16:00:00", "-", "INFO", false),
                        row(4L, "2026-06-19T17:00:00", "APPREAD", "FATAL", true),
                        row(5L, "2026-06-19T18:00:00", "-", "WARNING", false)
                ));
        // 분석 결과: 긴급 1 + 높음 1 = 분석 완료 2건, logId 4만 분석됨
        given(logAnalysisRepository.countByRiskLevelInRange(any(), any()))
                .willReturn(List.of(riskCount("긴급", 1), riskCount("높음", 1)));
        given(logAnalysisRepository.findAnalyzedLogIds(any()))
                .willReturn(List.of(4L));

        mockMvc.perform(get(DASHBOARD_SUMMARY_URL)
                        .param("startAt", "2026-06-19T09:00:00")
                        .param("endAt", "2026-06-19T21:00:00")
                        .param("interval", "6h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.totalLogCount").value(5))
                .andExpect(jsonPath("$.stats.cautionLogCount").value(2))
                .andExpect(jsonPath("$.stats.analyzedLogCount").value(2))
                .andExpect(jsonPath("$.timeSeries.length()").value(2))
                .andExpect(jsonPath("$.timeSeries[0].totalCount").value(2))
                .andExpect(jsonPath("$.timeSeries[0].cautionCount").value(1))
                .andExpect(jsonPath("$.timeSeries[1].totalCount").value(3))
                .andExpect(jsonPath("$.timeSeries[1].cautionCount").value(1))
                // riskDistribution: 4단계 고정 순서, 없는 등급 0
                .andExpect(jsonPath("$.riskDistribution.length()").value(4))
                .andExpect(jsonPath("$.riskDistribution[0].riskLevel").value("긴급"))
                .andExpect(jsonPath("$.riskDistribution[0].count").value(1))
                .andExpect(jsonPath("$.riskDistribution[2].riskLevel").value("보통"))
                .andExpect(jsonPath("$.riskDistribution[2].count").value(0))
                .andExpect(jsonPath("$.recentCautionLogs.length()").value(2))
                .andExpect(jsonPath("$.recentCautionLogs[0].label").value("APPREAD")) // 최신순(17:00)
                .andExpect(jsonPath("$.recentCautionLogs[0].isAnalysis").value(true)) // logId 4 분석됨
                .andExpect(jsonPath("$.recentCautionLogs[1].isAnalysis").value(false));
    }

    private static LogAnalysisRepository.RiskLevelCount riskCount(String riskLevel, long count) {
        return new LogAnalysisRepository.RiskLevelCount() {
            @Override
            public String getRiskLevel() {
                return riskLevel;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    private static BglLog row(long id, String occurredAt, String label, String logLevel, boolean fatal) {
        return BglLog.builder()
                .id(id)
                .occurredAt(LocalDateTime.parse(occurredAt))
                .label(label)
                .node("R01-M0-N0")
                .component("KERNEL")
                .logType("RAS")
                .logLevel(logLevel)
                .content("test log")
                .isFatal(fatal)
                .build();
    }
}
