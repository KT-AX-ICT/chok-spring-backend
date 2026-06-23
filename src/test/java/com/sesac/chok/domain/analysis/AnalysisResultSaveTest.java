package com.sesac.chok.domain.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sesac.chok.domain.analysis.dto.AnalysisResultCommand;
import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.analysis.service.AnalysisService;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import com.sesac.chok.global.error.NotFoundException;
import com.sesac.chok.global.type.Domain;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * 분석 결과 적재(write) 검증.
 * <p>FastAPI 분석 응답을 도메인 입력({@link AnalysisResultCommand})으로 받아
 * {@code log_analysis}에 영속화한다(저장 책임 = Analysis 도메인 서비스).
 */
@SpringBootTest
@Transactional
class AnalysisResultSaveTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private LogAnalysisRepository logAnalysisRepository;

    @Autowired
    private BglLogRepository bglLogRepository;

    private static final LocalDateTime ANALYZED_AT = LocalDateTime.of(2026, 6, 22, 9, 0, 0);

    private BglLog persistTargetLog() {
        return bglLogRepository.save(BglLog.builder()
                .occurredAt(LocalDateTime.of(2026, 6, 18, 8, 30, 0))
                .node("R02-M1-N0-C:J12-U11")
                .component("KERNEL")
                .logType("RAS")
                .logLevel("FATAL")
                .label("KERNDTLB")
                .content("data TLB error interrupt")
                .build());
    }

    @Test
    void persistsAnalysisLinkedToTargetLog() {
        BglLog target = persistTargetLog();
        AnalysisResultCommand command = new AnalysisResultCommand(
                target.getId(),
                Domain.BGL,
                "높음",
                "커널 데이터 TLB 오류 반복",
                "동일 노드에서 단시간 다수 TLB 오류 발생",
                "[\"노드 격리\", \"메모리 진단\"]",
                7L,
                ANALYZED_AT,
                true);

        Long savedId = analysisService.saveAnalysisResult(command);

        LogAnalysis saved = logAnalysisRepository.findById(savedId).orElseThrow();
        assertThat(saved.getLog().getId()).isEqualTo(target.getId());
        assertThat(saved.getDomain()).isEqualTo(Domain.BGL);
        assertThat(saved.getRiskLevel()).isEqualTo("높음");
        assertThat(saved.getSummary()).isEqualTo("커널 데이터 TLB 오류 반복");
        assertThat(saved.getAnalysis()).isEqualTo("동일 노드에서 단시간 다수 TLB 오류 발생");
        assertThat(saved.getAction()).isEqualTo("[\"노드 격리\", \"메모리 진단\"]");
        assertThat(saved.getClusterId()).isEqualTo(7L);
        assertThat(saved.getAnalyzedAt()).isEqualTo(ANALYZED_AT);
        assertThat(saved.getCreatedAt()).isNotNull(); // @CreationTimestamp 자동 채움
    }

    @Test
    void fallsBackToNowWhenAnalyzedAtMissing() {
        BglLog target = persistTargetLog();
        LocalDateTime before = LocalDateTime.now();
        AnalysisResultCommand command = new AnalysisResultCommand(
                target.getId(),
                Domain.BGL,
                "보통",
                "요약",
                "분석",
                "[]",
                99L, // 미분류 sentinel
                null, // batch 응답 누락 → Spring fallback(now)
                false);

        Long savedId = analysisService.saveAnalysisResult(command);

        LogAnalysis saved = logAnalysisRepository.findById(savedId).orElseThrow();
        assertThat(saved.getAnalyzedAt())
                .isNotNull()
                .isAfterOrEqualTo(before);
    }

    @Test
    void throwsNotFoundWhenTargetLogMissing() {
        AnalysisResultCommand command = new AnalysisResultCommand(
                999_999L,
                Domain.BGL,
                "낮음",
                "요약",
                "분석",
                "[]",
                99L,
                ANALYZED_AT,
                true);

        assertThatThrownBy(() -> analysisService.saveAnalysisResult(command))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updatesTargetLogAbnormalVerdict() {
        // 적재 직후엔 미분석(null) → 분석 결과 적재 시 대상 로그 is_abnormal이 판정값으로 갱신된다.
        BglLog target = persistTargetLog();
        assertThat(target.getIsAbnormal()).isNull();
        AnalysisResultCommand command = new AnalysisResultCommand(
                target.getId(),
                Domain.BGL,
                "높음",
                "요약",
                "분석",
                "[]",
                7L,
                ANALYZED_AT,
                false); // FATAL이지만 2차에서 정상 재분류

        analysisService.saveAnalysisResult(command);

        BglLog reloaded = bglLogRepository.findById(target.getId()).orElseThrow();
        assertThat(reloaded.getIsAbnormal()).isFalse();
    }

    @Test
    void rejectsNullClusterId() {
        // 미분류 건은 Python이 99로 채워 보낸다 → cluster_id는 NOT NULL. null 적재는 거부돼야 한다.
        BglLog target = persistTargetLog();
        LogAnalysis withNullCluster = LogAnalysis.builder()
                .log(target)
                .domain(Domain.BGL)
                .riskLevel("낮음")
                .summary("요약")
                .analysis("분석")
                .action("[]")
                .clusterId(null)
                .analyzedAt(ANALYZED_AT)
                .build();

        assertThatThrownBy(() -> logAnalysisRepository.saveAndFlush(withNullCluster))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
