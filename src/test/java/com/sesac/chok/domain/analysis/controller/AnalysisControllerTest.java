package com.sesac.chok.domain.analysis.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sesac.chok.domain.analysis.dto.AnalysisDto;
import com.sesac.chok.domain.analysis.dto.AnalysisSearchCondition;
import com.sesac.chok.domain.analysis.service.AnalysisService;
import com.sesac.chok.global.dto.PageResponse;
import com.sesac.chok.global.type.Domain;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalysisController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisService analysisService;

    @Test
    void returnsAnalysisListWithContentKey() throws Exception {
        AnalysisDto.LogInfo logInfo = new AnalysisDto.LogInfo(
                1001L, LocalDateTime.of(2026, 6, 18, 8, 30, 0), "R02-M1-N0-C:J12-U11",
                "KERNEL", "RAS", "FATAL", "data TLB error interrupt", true);
        AnalysisDto dto = new AnalysisDto(
                501L, Domain.BGL, "높음", 0L, "메모리/인터럽트군",
                "커널 데이터 TLB 오류 반복", "동일 노드 다수 발생",
                List.of("노드 격리/점검", "메모리 컨트롤러 진단"), logInfo);
        given(analysisService.getAnalysisList(any(AnalysisSearchCondition.class), any(Pageable.class)))
                .willReturn(new PageResponse<>(List.of(dto), 0, 50, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].analysisId").value(501))
                .andExpect(jsonPath("$.content[0].domain").value("BGL"))
                .andExpect(jsonPath("$.content[0].riskLevel").value("높음"))
                .andExpect(jsonPath("$.content[0].clusterId").value(0))
                .andExpect(jsonPath("$.content[0].patternName").value("메모리/인터럽트군"))
                .andExpect(jsonPath("$.content[0].aiSummary").value("커널 데이터 TLB 오류 반복"))
                .andExpect(jsonPath("$.content[0].responsePlan[0]").value("노드 격리/점검"))
                .andExpect(jsonPath("$.content[0].log.logId").value(1001))
                .andExpect(jsonPath("$.content[0].log.occurredAt").value("2026-06-18T08:30:00"))
                .andExpect(jsonPath("$.content[0].log.node").value("R02-M1-N0-C:J12-U11"))
                .andExpect(jsonPath("$.content[0].log.component").value("KERNEL"))
                .andExpect(jsonPath("$.content[0].log.logLevel").value("FATAL"))
                .andExpect(jsonPath("$.content[0].log.label").doesNotExist())
                .andExpect(jsonPath("$.content[0].log.content").value("data TLB error interrupt"))
                .andExpect(jsonPath("$.content[0].log.isCaution").value(true))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }
}
