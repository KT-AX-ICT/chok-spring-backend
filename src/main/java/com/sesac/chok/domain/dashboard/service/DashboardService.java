package com.sesac.chok.domain.dashboard.service;

import com.sesac.chok.domain.analysis.repository.LogAnalysisRepository;
import com.sesac.chok.domain.dashboard.dto.DashboardResponse;
import com.sesac.chok.domain.log.entity.BglLog;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 대시보드 집계 서비스.
 *
 * <p>{@link BglLogRepository}에서 {@code [startAt, endAt)} 범위의 로그를 한 번 조회해, 그 위에서
 * 시간대 버킷팅·label 기반 caution 카운트·key별 분포·최근 주의 로그를 Java로 집계한다.
 * interval이 가변(1h/5m/1d)이고 H2·MySQL을 모두 쓰므로 DB 날짜함수 대신 Java 버킷팅을 쓴다(이식성).
 *
 * <p>BglLog로 만드는 필드(총/주의 카운트, timeSeries, type/component/level 분포)와
 * log_analysis로 만드는 필드(riskDistribution, analyzedLogCount, recentCautionLog.isAnalysis)를
 * 각각 실집계한다. recentPatterns는 pattern 도메인 연결 전까지 mock으로 둔다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String NORMAL_LABEL = "-";
    private static final int RECENT_CAUTION_LIMIT = 5;
    private static final int MAX_BUCKETS = 200;
    // 도넛 고정 순서/구성 (PROJECT_CONTEXT riskLevel ENUM). 데이터에 없는 등급도 0으로 노출.
    private static final List<String> RISK_LEVELS = List.of("긴급", "높음", "보통", "낮음");

    private final BglLogRepository bglLogRepository;
    private final LogAnalysisRepository logAnalysisRepository;

    public DashboardResponse getDashboard(LocalDateTime startAt, LocalDateTime endAt, String interval) {
        LocalDateTime calculatedEndAt = endAt != null ? endAt : LocalDateTime.now().withNano(0);
        LocalDateTime calculatedStartAt = startAt != null ? startAt : calculatedEndAt.minusHours(24);
        String calculatedInterval = interval != null && !interval.isBlank() ? interval : "1h";

        List<BglLog> rows = bglLogRepository
                .findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(
                        calculatedStartAt, calculatedEndAt);
        List<LogAnalysisRepository.RiskLevelCount> riskCounts =
                logAnalysisRepository.countByRiskLevelInRange(calculatedStartAt, calculatedEndAt);
        // 분석 완료 수: 범위 내 log_analysis row 합계 (로그당 분석 1건 가정).
        int analyzedCount = riskCounts.stream().mapToInt(r -> (int) r.getCount()).sum();

        log.info(
                "[Dashboard] aggregated from {} bgl_log rows, {} analyses, startAt={}, endAt={}, interval={}",
                rows.size(), analyzedCount, calculatedStartAt, calculatedEndAt, calculatedInterval
        );

        return new DashboardResponse(
                range(calculatedStartAt, calculatedEndAt),
                aggregateStats(rows, analyzedCount),
                aggregateTimeSeries(rows, calculatedStartAt, calculatedEndAt, calculatedInterval),
                aggregateRiskDistribution(riskCounts),
                aggregateTypeDistribution(rows),
                aggregateComponentDistribution(rows),
                aggregateLevelDistribution(rows),
                aggregateRecentCautionLogs(rows),
                mockRecentPatterns()
        );
    }

    // ---------------------------------------------------------------------
    // 집계 로직 (BglLog 단일 입력 기준)
    // ---------------------------------------------------------------------

    private static boolean isCaution(String label) {
        return label != null && !NORMAL_LABEL.equals(label);
    }

    private DashboardResponse.Stats aggregateStats(List<BglLog> rows, int analyzedCount) {
        int total = rows.size();
        int caution = (int) rows.stream().filter(r -> isCaution(r.getLabel())).count();
        return new DashboardResponse.Stats(total, caution, analyzedCount);
    }

    /** log_analysis.risk_level GROUP BY 결과를 4단계 고정 순서(없는 등급 0)로 구성한다. */
    private List<DashboardResponse.RiskDistributionItem> aggregateRiskDistribution(
            List<LogAnalysisRepository.RiskLevelCount> riskCounts) {
        Map<String, Integer> byLevel = new HashMap<>();
        for (LogAnalysisRepository.RiskLevelCount c : riskCounts) {
            if (c.getRiskLevel() != null) {
                byLevel.merge(c.getRiskLevel(), (int) c.getCount(), Integer::sum);
            }
        }
        LinkedHashMap<String, Integer> ordered = new LinkedHashMap<>();
        RISK_LEVELS.forEach(level -> ordered.put(level, byLevel.getOrDefault(level, 0)));
        byLevel.forEach(ordered::putIfAbsent); // 예상 외 등급도 누락 없이 노출
        return ordered.entrySet().stream()
                .map(e -> new DashboardResponse.RiskDistributionItem(e.getKey(), e.getValue()))
                .toList();
    }

    /**
     * 시간 버킷 집계. 동등 SQL 예시:
     * <pre>
     * SELECT bucket(occurred_at, :interval) AS time,
     *        COUNT(*)                       AS totalCount,
     *        SUM(CASE WHEN label &lt;&gt; '-' THEN 1 ELSE 0 END) AS cautionCount
     * FROM bgl_log
     * WHERE occurred_at &gt;= :startAt AND occurred_at &lt; :endAt
     * GROUP BY time ORDER BY time
     * </pre>
     * 빈 버킷도 0으로 노출해야 line/area 차트가 빈 구간에서 바닥(0)으로 내려간다(보간 방지).
     */
    private List<DashboardResponse.TimeSeriesItem> aggregateTimeSeries(
            List<BglLog> rows, LocalDateTime startAt, LocalDateTime endAt, String interval) {
        Duration bucket = parseInterval(interval);

        // 빈 버킷도 0으로 노출하기 위한 스켈레톤. [startAt, endAt) 기준이라 endAt에서 시작하는 버킷은 제외.
        List<LocalDateTime> bucketStarts = new ArrayList<>();
        LocalDateTime at = startAt;
        while (at.isBefore(endAt) && bucketStarts.size() < MAX_BUCKETS) {
            bucketStarts.add(at);
            at = at.plus(bucket);
        }
        if (bucketStarts.isEmpty()) {
            bucketStarts.add(startAt);
        }

        int n = bucketStarts.size();
        int[] total = new int[n];
        int[] caution = new int[n];
        long bucketSeconds = Math.max(bucket.getSeconds(), 1);

        for (BglLog r : rows) {
            LocalDateTime occurredAt = r.getOccurredAt();
            if (occurredAt == null || occurredAt.isBefore(startAt) || !occurredAt.isBefore(endAt)) {
                continue;
            }
            int idx = (int) (Duration.between(startAt, occurredAt).getSeconds() / bucketSeconds);
            if (idx < 0 || idx >= n) {
                continue;
            }
            total[idx]++;
            if (isCaution(r.getLabel())) {
                caution[idx]++;
            }
        }

        List<DashboardResponse.TimeSeriesItem> items = new ArrayList<>(n);
        for (int k = 0; k < n; k++) {
            items.add(new DashboardResponse.TimeSeriesItem(format(bucketStarts.get(k)), total[k], caution[k]));
        }
        return items;
    }

    private List<DashboardResponse.TypeDistributionItem> aggregateTypeDistribution(List<BglLog> rows) {
        return countByDesc(rows, BglLog::getLogType).entrySet().stream()
                .map(e -> new DashboardResponse.TypeDistributionItem(e.getKey(), e.getValue()))
                .toList();
    }

    private List<DashboardResponse.ComponentDistributionItem> aggregateComponentDistribution(List<BglLog> rows) {
        return countByDesc(rows, BglLog::getComponent).entrySet().stream()
                .map(e -> new DashboardResponse.ComponentDistributionItem(e.getKey(), e.getValue()))
                .toList();
    }

    private List<DashboardResponse.LevelDistributionItem> aggregateLevelDistribution(List<BglLog> rows) {
        return countByDesc(rows, BglLog::getLogLevel).entrySet().stream()
                .map(e -> new DashboardResponse.LevelDistributionItem(e.getKey(), e.getValue()))
                .toList();
    }

    private List<DashboardResponse.RecentCautionLog> aggregateRecentCautionLogs(List<BglLog> rows) {
        List<BglLog> recent = rows.stream()
                .filter(r -> isCaution(r.getLabel()) && r.getOccurredAt() != null)
                .sorted(Comparator.comparing(BglLog::getOccurredAt).reversed())
                .limit(RECENT_CAUTION_LIMIT)
                .toList();

        // isAnalysis: 해당 로그에 log_analysis row가 있는지 일괄 조회
        List<Long> logIds = recent.stream().map(BglLog::getId).filter(Objects::nonNull).toList();
        Set<Long> analyzedIds = logIds.isEmpty()
                ? Set.of()
                : new HashSet<>(logAnalysisRepository.findAnalyzedLogIds(logIds));

        return recent.stream()
                .map(r -> new DashboardResponse.RecentCautionLog(
                        r.getId(), format(r.getOccurredAt()), r.getNode(), r.getComponent(),
                        r.getLogLevel(), r.getLogType(), r.getLabel(), true,
                        analyzedIds.contains(r.getId()), r.getContent()))
                .toList();
    }

    /** key별 count를 내림차순으로 정렬해 반환. 동등 SQL: {@code GROUP BY key ORDER BY COUNT(*) DESC}. */
    private static LinkedHashMap<String, Integer> countByDesc(List<BglLog> rows, Function<BglLog, String> key) {
        Map<String, Integer> counts = new HashMap<>();
        for (BglLog r : rows) {
            String k = key.apply(r);
            if (k != null) {
                counts.merge(k, 1, Integer::sum);
            }
        }
        LinkedHashMap<String, Integer> ordered = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> ordered.put(e.getKey(), e.getValue()));
        return ordered;
    }

    // ---------------------------------------------------------------------
    // 범위 echo + analysis/pattern 파생 mock (해당 도메인 연결 전까지 유지)
    // ---------------------------------------------------------------------

    private DashboardResponse.Range range(LocalDateTime startAt, LocalDateTime endAt) {
        return new DashboardResponse.Range(format(startAt), format(endAt));
    }

    // TODO: Remove after PatternService recent pattern query is implemented.
    private List<DashboardResponse.RecentPattern> mockRecentPatterns() {
        return List.of(
                new DashboardResponse.RecentPattern(12L, "Data TLB Error", 87, "높음", 90),
                new DashboardResponse.RecentPattern(13L, "Application Read Error", 42, "보통", 72)
        );
    }

    // ---------------------------------------------------------------------
    // interval 파싱 헬퍼
    // ---------------------------------------------------------------------

    private Duration parseInterval(String interval) {
        String normalized = interval.toLowerCase();
        try {
            if (normalized.endsWith("m")) {
                return positiveOrDefault(Duration.ofMinutes(parseIntervalAmount(normalized)));
            }
            if (normalized.endsWith("h")) {
                return positiveOrDefault(Duration.ofHours(parseIntervalAmount(normalized)));
            }
            if (normalized.endsWith("d")) {
                return positiveOrDefault(Duration.ofDays(parseIntervalAmount(normalized)));
            }
        } catch (NumberFormatException ignored) {
            log.info("[Dashboard] unsupported interval value, fallback to 1h. interval={}", interval);
        }
        return Duration.ofHours(1);
    }

    private long parseIntervalAmount(String normalizedInterval) {
        return Long.parseLong(normalizedInterval.substring(0, normalizedInterval.length() - 1));
    }

    private Duration positiveOrDefault(Duration duration) {
        if (duration.isPositive()) {
            return duration;
        }
        log.info("[Dashboard] non-positive interval value, fallback to 1h.");
        return Duration.ofHours(1);
    }

    private String format(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}
