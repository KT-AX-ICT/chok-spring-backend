package com.sesac.chok.domain.log;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
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
 * `GET /logs` 다중 필터 + 페이지네이션 end-to-end 통합 테스트(§3.2).
 * <p>{@code bgl_log} LEFT JOIN {@code log_analysis}로 {@code riskLevel}/{@code isAnalysis}를 함께 검증한다.
 * DataInitializer가 적재한 시연 seed(2,000건)는 비우고 테스트 데이터만 검증한다(@Transactional 롤백).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class LogSearchIntegrationTest {

    private static final String LOGS = "/api/v1/logs";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BglLogRepository bglLogRepository;

    @Autowired
    private LogAnalysisRepository logAnalysisRepository;

    private BglLog logA; // 08:00 KERNEL FATAL KERNDTLB, 분석O(높음)
    private BglLog logB; // 09:00 MMCS   INFO  "-",      분석X
    private BglLog logC; // 10:00 KERNEL WARN  APPREAD,  분석X

    @BeforeEach
    void setUp() {
        logAnalysisRepository.deleteAllInBatch();
        bglLogRepository.deleteAllInBatch();

        logA = save(LocalDateTime.of(2026, 6, 18, 8, 0), "node-A", "KERNEL", "RAS", "FATAL",
                "KERNDTLB", "data TLB error interrupt");
        logB = save(LocalDateTime.of(2026, 6, 18, 9, 0), "node-B", "MMCS", "RAS", "INFO",
                "-", "generating core.123 file");
        logC = save(LocalDateTime.of(2026, 6, 18, 10, 0), "node-C", "KERNEL", "RAS", "WARN",
                "APPREAD", "ddr error threshold exceeded");

        logAnalysisRepository.save(LogAnalysis.builder()
                .log(logA).domain(Domain.BGL).riskLevel("높음")
                .summary("요약").analysis("분석").action("[]").clusterId(1L)
                .analyzedAt(LocalDateTime.of(2026, 6, 18, 8, 30)).build());
    }

    private BglLog save(LocalDateTime occurredAt, String node, String component,
            String logType, String logLevel, String label, String content) {
        return bglLogRepository.save(BglLog.builder()
                .occurredAt(occurredAt).node(node).component(component)
                .logType(logType).logLevel(logLevel).label(label).content(content).build());
    }

    @Test
    void returnsAllLogsSortedByOccurredAtDescWithDerivedFlags() throws Exception {
        mockMvc.perform(get(LOGS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3))
                // 기본 정렬 occurredAt desc → C(10시), B(9시), A(8시)
                .andExpect(jsonPath("$.content[0].logId").value(logC.getId()))
                .andExpect(jsonPath("$.content[2].logId").value(logA.getId()))
                // 분석된 logA: isAnalysis/isCaution true, riskLevel 노출
                .andExpect(jsonPath("$.content[2].isAnalysis").value(true))
                .andExpect(jsonPath("$.content[2].isCaution").value(true))
                .andExpect(jsonPath("$.content[2].riskLevel").value("높음"))
                .andExpect(jsonPath("$.content[2].component").value("KERNEL"))
                // 미분석 logB(label '-'): isAnalysis/isCaution false
                .andExpect(jsonPath("$.content[1].isAnalysis").value(false))
                .andExpect(jsonPath("$.content[1].isCaution").value(false));
    }

    @Test
    void filtersByComponent() throws Exception {
        mockMvc.perform(get(LOGS).param("component", "KERNEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].logId").value(logC.getId()))
                .andExpect(jsonPath("$.content[1].logId").value(logA.getId()));
    }

    @Test
    void filtersByOccurredAtRange() throws Exception {
        mockMvc.perform(get(LOGS)
                        .param("startAt", "2026-06-18T08:30:00")
                        .param("endAt", "2026-06-18T09:30:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].logId").value(logB.getId()));
    }

    @Test
    void filtersByRiskLevel() throws Exception {
        mockMvc.perform(get(LOGS).param("riskLevel", "높음"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].logId").value(logA.getId()));
    }

    @Test
    void filtersByIsAnalysisFalse() throws Exception {
        mockMvc.perform(get(LOGS).param("isAnalysis", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].logId").value(logC.getId()))
                .andExpect(jsonPath("$.content[1].logId").value(logB.getId()));
    }

    @Test
    void filtersByIsCautionFalse() throws Exception {
        mockMvc.perform(get(LOGS).param("isCaution", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].logId").value(logB.getId()));
    }

    @Test
    void filtersByKeyword() throws Exception {
        mockMvc.perform(get(LOGS).param("keyword", "TLB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].logId").value(logA.getId()));
    }

    @Test
    void paginatesWithPageAndSize() throws Exception {
        mockMvc.perform(get(LOGS).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }
}
