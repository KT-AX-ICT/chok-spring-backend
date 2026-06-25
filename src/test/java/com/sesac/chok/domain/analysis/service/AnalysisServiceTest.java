package com.sesac.chok.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.sesac.chok.domain.analysis.dto.AnalysisDto;
import com.sesac.chok.domain.analysis.dto.AnalysisSearchCondition;
import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.pattern.entity.PatternView;
import com.sesac.chok.domain.pattern.repository.PatternViewRepository;
import com.sesac.chok.global.dto.PageResponse;
import com.sesac.chok.global.type.Domain;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private LogAnalysisRepository logAnalysisRepository;

    @Mock
    private PatternViewRepository patternViewRepository;

    @InjectMocks
    private AnalysisService analysisService;

    @Test
    void mapsAnalysisWithNestedLogAndParsesResponsePlan() {
        BglLog log = BglLog.builder()
                .id(1001L)
                .occurredAt(LocalDateTime.of(2026, 6, 18, 8, 30, 0))
                .node("R02-M1-N0-C:J12-U11")
                .component("KERNEL")
                .logType("RAS")
                .logLevel("FATAL")
                .label("KERNDTLB")
                .content("data TLB error interrupt")
                .build();
        LogAnalysis entity = LogAnalysis.builder()
                .id(501L)
                .log(log)
                .domain(Domain.BGL)
                .riskLevel("높음")
                .summary("커널 데이터 TLB 오류 반복")
                .analysis("동일 노드에서 단시간 다수 TLB 오류 발생")
                .action("[\"노드 격리/점검\", \"메모리 컨트롤러 진단\"]")
                .clusterId(12L)
                .analyzedAt(LocalDateTime.of(2026, 6, 18, 8, 31, 0))
                .createdAt(LocalDateTime.of(2026, 6, 18, 8, 31, 5))
                .build();
        Pageable pageable = PageRequest.of(0, 50);
        given(logAnalysisRepository.search(null, null, null, null, pageable))
                .willReturn(new PageImpl<>(List.of(entity), pageable, 1));
        given(patternViewRepository.findAllById(List.of(12L)))
                .willReturn(List.of(PatternView.builder().id(12L).patternName("Data TLB Error").build()));

        PageResponse<AnalysisDto> result = analysisService.getAnalysisList(new AnalysisSearchCondition(null, null, null, null), pageable);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);

        AnalysisDto dto = result.content().get(0);
        assertThat(dto.analysisId()).isEqualTo(501L);
        assertThat(dto.domain()).isEqualTo(Domain.BGL);
        assertThat(dto.riskLevel()).isEqualTo("높음");
        assertThat(dto.clusterId()).isEqualTo(12L);
        assertThat(dto.patternName()).isEqualTo("Data TLB Error");
        assertThat(dto.aiSummary()).isEqualTo("커널 데이터 TLB 오류 반복");
        assertThat(dto.analysis()).isEqualTo("동일 노드에서 단시간 다수 TLB 오류 발생");
        assertThat(dto.responsePlan()).containsExactly("노드 격리/점검", "메모리 컨트롤러 진단");

        AnalysisDto.LogInfo logInfo = dto.log();
        assertThat(logInfo.logId()).isEqualTo(1001L);
        assertThat(logInfo.occurredAt()).isEqualTo(LocalDateTime.of(2026, 6, 18, 8, 30, 0));
        assertThat(logInfo.node()).isEqualTo("R02-M1-N0-C:J12-U11");
        assertThat(logInfo.component()).isEqualTo("KERNEL");
        assertThat(logInfo.logType()).isEqualTo("RAS");
        assertThat(logInfo.logLevel()).isEqualTo("FATAL");
        assertThat(logInfo.content()).isEqualTo("data TLB error interrupt");
        assertThat(logInfo.isCaution()).isTrue(); // FATAL 미분석(2차 전) → 1차 안전망으로 주의
    }

    @Test
    void marksLogAsNotCautionWhenSecondPassNormal() {
        // FATAL이어도 2차가 정상(isAbnormal=false)으로 판정하면 비주의(2차 우선).
        BglLog log = BglLog.builder().id(2002L).logLevel("FATAL").isAbnormal(false)
                .occurredAt(LocalDateTime.of(2026, 6, 18, 9, 0, 0)).build();
        LogAnalysis entity = LogAnalysis.builder()
                .id(502L).log(log).domain(Domain.BGL).riskLevel("낮음")
                .summary("정상").analysis("이상 없음").action("점검 불필요")
                .analyzedAt(LocalDateTime.of(2026, 6, 18, 9, 1, 0))
                .createdAt(LocalDateTime.of(2026, 6, 18, 9, 1, 0))
                .build();
        Pageable pageable = PageRequest.of(0, 50);
        given(logAnalysisRepository.search(null, null, null, null, pageable))
                .willReturn(new PageImpl<>(List.of(entity), pageable, 1));

        AnalysisDto dto = analysisService.getAnalysisList(new AnalysisSearchCondition(null, null, null, null), pageable).content().get(0);

        assertThat(dto.log().isCaution()).isFalse();
        assertThat(dto.responsePlan()).containsExactly("점검 불필요");
    }
}
