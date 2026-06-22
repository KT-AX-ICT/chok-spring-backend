package com.sesac.chok.domain.dashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isIn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesac.chok.domain.dashboard.service.DashboardService;
import com.sesac.chok.global.config.CorsConfig;
import com.sesac.chok.global.config.SecurityConfig;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(DashboardController.class)
@Import({DashboardService.class, SecurityConfig.class, CorsConfig.class})
class DashboardControllerTest {

    private static final String DASHBOARD_SUMMARY_URL = "/api/v1/dashboard/summary";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

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
}
