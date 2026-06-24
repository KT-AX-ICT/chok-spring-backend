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

    private BglLog logA; // 08:00 KERNEL FATAL, isAbnormal=true(2차 이상), 분석O(높음) → isCaution true
    private BglLog logB; // 09:00 MMCS   INFO,  isAbnormal=null(미분석), 분석X     → isCaution false
    private BglLog logC; // 10:00 KERNEL WARN,  isAbnormal=null(미분석), 분석X     → isCaution false

    @BeforeEach
    void setUp() {
        logAnalysisRepository.deleteAllInBatch();
        bglLogRepository.deleteAllInBatch();

        logA = save(LocalDateTime.of(2026, 6, 18, 8, 0), "node-A", "KERNEL", "RAS", "FATAL",
                "KERNDTLB", true, "data TLB error interrupt");
        logB = save(LocalDateTime.of(2026, 6, 18, 9, 0), "node-B", "MMCS", "RAS", "INFO",
                "-", null, "generating core.123 file");
        logC = save(LocalDateTime.of(2026, 6, 18, 10, 0), "node-C", "KERNEL", "RAS", "WARN",
                "APPREAD", null, "ddr error threshold exceeded");

        logAnalysisRepository.save(LogAnalysis.builder()
                .log(logA).domain(Domain.BGL).riskLevel("높음")
                .summary("요약").analysis("분석").action("[]").clusterId(1L)
                .analyzedAt(LocalDateTime.of(2026, 6, 18, 8, 30)).build());
    }

    private BglLog save(LocalDateTime occurredAt, String node, String component,
            String logType, String logLevel, String label, Boolean isAbnormal, String content) {
        return bglLogRepository.save(BglLog.builder()
                .occurredAt(occurredAt).node(node).component(component)
                .logType(logType).logLevel(logLevel).label(label).isAbnormal(isAbnormal)
                .content(content).build());
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
                // logA(isAbnormal=true): isAnalysis/isCaution true, riskLevel 노출
                .andExpect(jsonPath("$.content[2].isAnalysis").value(true))
                .andExpect(jsonPath("$.content[2].isCaution").value(true))
                .andExpect(jsonPath("$.content[2].riskLevel").value("높음"))
                .andExpect(jsonPath("$.content[2].component").value("KERNEL"))
                // logB(미분석 비FATAL): isAnalysis/isCaution false
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
        // 미분석 비FATAL(logB INFO, logC WARN)만 비주의 — label과 무관
        mockMvc.perform(get(LOGS).param("isCaution", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].logId").value(logC.getId()))
                .andExpect(jsonPath("$.content[1].logId").value(logB.getId()));
    }

    @Test
    void flagsUnanalyzedFatalAsCaution() throws Exception {
        // 1차 안전망: 2차 전(isAbnormal=null)이라도 FATAL이면 주의. label '-'이어도 무관.
        BglLog fatalPending = save(LocalDateTime.of(2026, 6, 18, 11, 0), "node-D", "KERNEL",
                "RAS", "FATAL", "-", null, "machine check interrupt");

        mockMvc.perform(get(LOGS).param("isCaution", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)) // logA(이상) + fatalPending(FATAL 미분석)
                .andExpect(jsonPath("$.content[0].logId").value(fatalPending.getId()))
                .andExpect(jsonPath("$.content[0].isCaution").value(true));
    }

    @Test
    void clearsCautionWhenSecondPassMarksFatalNormal() throws Exception {
        // 2차 우선: FATAL이어도 isAbnormal=false(정상 판정)면 비주의로 내려간다.
        BglLog fatalCleared = save(LocalDateTime.of(2026, 6, 18, 12, 0), "node-E", "KERNEL",
                "RAS", "FATAL", "KERNDTLB", false, "recovered ecc error");

        mockMvc.perform(get(LOGS).param("logLevel", "FATAL").param("isCaution", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].logId").value(fatalCleared.getId()))
                .andExpect(jsonPath("$.content[0].isCaution").value(false));
    }

    @Test
    void flagsAnalyzedAbnormalNonFatalAsCaution() throws Exception {
        // 비FATAL이라도 2차가 이상(isAbnormal=true)으로 판정하면 주의.
        BglLog abnormalInfo = save(LocalDateTime.of(2026, 6, 18, 13, 0), "node-F", "MMCS",
                "RAS", "INFO", "-", true, "anomalous pattern detected");

        mockMvc.perform(get(LOGS).param("logLevel", "INFO").param("isCaution", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].logId").value(abnormalInfo.getId()))
                .andExpect(jsonPath("$.content[0].isCaution").value(true));
    }

    @Test
    void responseOmitsLabelField() throws Exception {
        // label(답지)은 응답 페이로드에서 제거 — 프론트로 내려가지 않는다.
        mockMvc.perform(get(LOGS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].label").doesNotExist());
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
