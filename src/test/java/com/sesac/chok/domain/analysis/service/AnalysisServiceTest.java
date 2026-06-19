package com.sesac.chok.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.sesac.chok.domain.analysis.dto.AnalysisDto;
import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.global.dto.PageResponse;
import com.sesac.chok.global.type.Domain;
import com.sesac.chok.global.type.RiskLevel;
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

    @InjectMocks
    private AnalysisService analysisService;

    @Test
    void mapsAnalysisWithNestedLogAndParsesResponsePlan() {
        BglLog log = BglLog.builder()
                .id(1001L)
                .logTs(LocalDateTime.of(2026, 6, 18, 8, 30, 0))
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
                .riskLevel(RiskLevel.HIGH)
                .summary("커널 데이터 TLB 오류 반복")
                .analysis("동일 노드에서 단시간 다수 TLB 오류 발생")
                .action("[\"노드 격리/점검\", \"메모리 컨트롤러 진단\"]")
                .clusterId(12L)
                .analyzedAt(LocalDateTime.of(2026, 6, 18, 8, 31, 0))
                .createdAt(LocalDateTime.of(2026, 6, 18, 8, 31, 5))
                .build();
        Pageable pageable = PageRequest.of(0, 50);
        given(logAnalysisRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(entity), pageable, 1));

        PageResponse<AnalysisDto> result = analysisService.getAnalysisList(pageable);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);

        AnalysisDto dto = result.content().get(0);
        assertThat(dto.analysisId()).isEqualTo(501L);
        assertThat(dto.domain()).isEqualTo(Domain.BGL);
        assertThat(dto.riskLevel()).isEqualTo(RiskLevel.HIGH);
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
        assertThat(logInfo.label()).isEqualTo("KERNDTLB");
        assertThat(logInfo.content()).isEqualTo("data TLB error interrupt");
        assertThat(logInfo.isCaution()).isTrue();
    }

    @Test
    void marksLogAsNotCautionWhenLabelIsDash() {
        BglLog log = BglLog.builder().id(2002L).label("-")
                .logTs(LocalDateTime.of(2026, 6, 18, 9, 0, 0)).build();
        LogAnalysis entity = LogAnalysis.builder()
                .id(502L).log(log).domain(Domain.BGL).riskLevel(RiskLevel.LOW)
                .summary("정상").analysis("이상 없음").action("점검 불필요")
                .analyzedAt(LocalDateTime.of(2026, 6, 18, 9, 1, 0))
                .createdAt(LocalDateTime.of(2026, 6, 18, 9, 1, 0))
                .build();
        Pageable pageable = PageRequest.of(0, 50);
        given(logAnalysisRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(entity), pageable, 1));

        AnalysisDto dto = analysisService.getAnalysisList(pageable).content().get(0);

        assertThat(dto.log().isCaution()).isFalse();
        assertThat(dto.responsePlan()).containsExactly("점검 불필요");
    }
}
