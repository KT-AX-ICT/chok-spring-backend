package com.sesac.chok.domain.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sesac.chok.domain.analysis.dto.AnalysisResultCommand;
import com.sesac.chok.domain.analysis.entity.LogAnalysis;
import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.analysis.service.AnalysisService;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.entity.BglTemplate;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import com.sesac.chok.domain.log.repository.BglTemplateRepository;
import com.sesac.chok.global.error.NotFoundException;
import com.sesac.chok.global.type.Domain;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    @Autowired
    private BglTemplateRepository bglTemplateRepository;

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
                true,
                null);

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
                false,
                null);

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
                true,
                null);

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
                false, // FATAL이지만 2차에서 정상 재분류
                null);

        analysisService.saveAnalysisResult(command);

        BglLog reloaded = bglLogRepository.findById(target.getId()).orElseThrow();
        assertThat(reloaded.getIsAbnormal()).isFalse();
    }

    @Test
    void setsEventIdWhenTemplateExists() {
        // 2차 event_id가 정본(bgl_template)에 있으면 대상 로그 event_id를 그 값으로 갱신한다.
        bglTemplateRepository.save(BglTemplate.builder()
                .eventId("E55").eventTemplate("data TLB error interrupt").build());
        BglLog target = persistTargetLog();
        AnalysisResultCommand command = new AnalysisResultCommand(
                target.getId(), Domain.BGL, "높음", "요약", "분석", "[]", 7L, ANALYZED_AT, true, "E55");

        analysisService.saveAnalysisResult(command);

        BglLog reloaded = bglLogRepository.findById(target.getId()).orElseThrow();
        assertThat(reloaded.getEventId()).isEqualTo("E55");
    }

    @Test
    void leavesEventIdNullWhenTemplateMissing() {
        // 정본에 없는 event_id면 적재는 그대로 진행하고 event_id만 비운다(관대 처리 — 한 건 미매칭이 적재를 깨지 않음).
        BglLog target = persistTargetLog();
        AnalysisResultCommand command = new AnalysisResultCommand(
                target.getId(), Domain.BGL, "높음", "요약", "분석", "[]", 7L, ANALYZED_AT, true, "E_UNKNOWN");

        Long savedId = analysisService.saveAnalysisResult(command);

        assertThat(logAnalysisRepository.findById(savedId)).isPresent(); // 분석 결과는 정상 저장
        BglLog reloaded = bglLogRepository.findById(target.getId()).orElseThrow();
        assertThat(reloaded.getEventId()).isNull();
    }

    @Test
    void leavesEventIdNullWhenResultEventIdIsNull() {
        // 정상 로그는 event_id 매칭이 없어 null로 온다(FastAPI 계약) → 그대로 null 유지.
        BglLog target = persistTargetLog();
        AnalysisResultCommand command = new AnalysisResultCommand(
                target.getId(), Domain.BGL, "낮음", "요약", "분석", "[]", 99L, ANALYZED_AT, false, null);

        analysisService.saveAnalysisResult(command);

        BglLog reloaded = bglLogRepository.findById(target.getId()).orElseThrow();
        assertThat(reloaded.getEventId()).isNull();
    }

    @Test
    void allowsNullClusterIdForNormalLogs() {
        // 정상 로그는 FastAPI가 clusterId=null로 전송(계약) → nullable 허용 확인.
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

        LogAnalysis saved = logAnalysisRepository.saveAndFlush(withNullCluster);
        assertThat(saved.getClusterId()).isNull();
    }
}
