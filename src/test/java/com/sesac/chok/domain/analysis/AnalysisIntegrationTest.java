package com.sesac.chok.domain.analysis;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import com.sesac.chok.global.type.Domain;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * `GET /analysis` end-to-end 통합 테스트.
 * 컨트롤러 → 서비스 → 리포지토리 → H2(인메모리) 전 구간을 목킹 없이 실제로 검증한다.
 * dev 프로필(기본) = H2 in-memory, ddl-auto:update 로 테이블 자동 생성. 별도 DB 파일 불필요.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class AnalysisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LogAnalysisRepository repository;

    @Autowired
    private BglLogRepository bglLogRepository;

    private static final LocalDateTime TS = LocalDateTime.of(2026, 6, 18, 8, 30, 0);

    private BglLog savedLog(String node, String label) {
        return bglLogRepository.save(BglLog.builder()
                .occurredAt(TS)
                .node(node)
                .component("KERNEL")
                .logType("RAS")
                .logLevel("FATAL")
                .label(label)
                .content("data TLB error interrupt")
                .createdAt(TS)
                .build());
    }

    private LogAnalysis.LogAnalysisBuilder base(BglLog log) {
        return LogAnalysis.builder()
                .log(log)
                .domain(Domain.BGL)
                .riskLevel("높음")
                .summary("요약")
                .analysis("분석")
                .action("[]")
                .clusterId(1L)
                .analyzedAt(TS)
                .createdAt(TS);
    }

    @Test
    void returnsPersistedRowsWithNestedLogSortedByAnalyzedAtDesc() throws Exception {
        // 저장 순서(id)와 분석 시점(analyzedAt)을 일부러 어긋나게 둔다.
        // 먼저 저장(낮은 id)이지만 분석은 더 나중(09:00) → analyzedAt,desc면 이 행이 맨 앞.
        BglLog newerLog = savedLog("node-NEWER", "KERNDTLB");
        LogAnalysis newer = repository.save(base(newerLog).summary("analyzed-later")
                .analyzedAt(LocalDateTime.of(2026, 6, 18, 9, 0, 0)).build());
        // 나중 저장(높은 id)이지만 분석은 더 이전(08:00).
        repository.save(base(savedLog("node-OLDER", "KERNDTLB")).summary("analyzed-earlier")
                .analyzedAt(LocalDateTime.of(2026, 6, 18, 8, 0, 0)).build());

        mockMvc.perform(get("/api/v1/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].analysisId").value(newer.getId()))
                .andExpect(jsonPath("$.content[0].aiSummary").value("analyzed-later"))
                .andExpect(jsonPath("$.content[0].domain").value("BGL"))
                .andExpect(jsonPath("$.content[0].riskLevel").value("높음"))
                .andExpect(jsonPath("$.content[0].log.logId").value(newerLog.getId()))
                .andExpect(jsonPath("$.content[0].log.node").value("node-NEWER"))
                .andExpect(jsonPath("$.content[0].log.occurredAt").value("2026-06-18T08:30:00"))
                .andExpect(jsonPath("$.content[0].log.isCaution").value(true))
                .andExpect(jsonPath("$.content[1].aiSummary").value("analyzed-earlier"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void marksLogAsNotCautionWhenLabelIsDash() throws Exception {
        repository.save(base(savedLog("node-N", "-")).build());

        mockMvc.perform(get("/api/v1/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].log.isCaution").value(false));
    }

    @Test
    void parsesJsonArrayAndNewlineActionFormats() throws Exception {
        // JSON 배열 포맷
        repository.save(base(savedLog("node-J", "KERNDTLB")).action("[\"노드 격리\", \"보드 교체\"]").build());

        mockMvc.perform(get("/api/v1/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].responsePlan.length()").value(2))
                .andExpect(jsonPath("$.content[0].responsePlan[0]").value("노드 격리"))
                .andExpect(jsonPath("$.content[0].responsePlan[1]").value("보드 교체"));

        repository.deleteAll();

        // 줄바꿈 구분 포맷
        repository.save(base(savedLog("node-K", "KERNDTLB")).action("점검 실행\n재시작\n로그 확인").build());

        mockMvc.perform(get("/api/v1/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].responsePlan.length()").value(3))
                .andExpect(jsonPath("$.content[0].responsePlan[0]").value("점검 실행"))
                .andExpect(jsonPath("$.content[0].responsePlan[2]").value("로그 확인"));
    }

    @Test
    void returnsEmptyContentWhenNoData() throws Exception {
        mockMvc.perform(get("/api/v1/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void respectsPageAndSizeParams() throws Exception {
        repository.save(base(savedLog("node-1", "KERNDTLB")).build());
        repository.save(base(savedLog("node-2", "KERNDTLB")).build());
        repository.save(base(savedLog("node-3", "KERNDTLB")).build());

        mockMvc.perform(get("/api/v1/analysis").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }
}
