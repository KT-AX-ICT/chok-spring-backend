package com.sesac.chok.domain.dashboard.service;

import com.sesac.chok.domain.dashboard.dto.DashboardResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DashboardService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public DashboardResponse getDashboard(LocalDateTime startAt, LocalDateTime endAt, String interval) {
        LocalDateTime calculatedEndAt = endAt != null ? endAt : LocalDateTime.now().withNano(0);
        LocalDateTime calculatedStartAt = startAt != null ? startAt : calculatedEndAt.minusHours(24);
        String calculatedInterval = interval != null && !interval.isBlank() ? interval : "1h";
        log.info(
                "[Dashboard] returning mock response - real aggregation pending, startAt={}, endAt={}, interval={}",
                calculatedStartAt,
                calculatedEndAt,
                calculatedInterval
        );

        // TODO: Replace mock data after LogService/AnalysisService/PatternService dashboard query methods are implemented.
        return new DashboardResponse(
                mockRange(calculatedStartAt, calculatedEndAt),
                mockStats(),
                mockTimeSeries(calculatedStartAt, calculatedEndAt, calculatedInterval),
                mockRiskDistribution(),
                mockTypeDistribution(),
                mockComponentDistribution(),
                mockLevelDistribution(),
                mockRecentCautionLogs(calculatedEndAt),
                mockRecentPatterns()
        );
    }

    // TODO: Remove after LogService range aggregation is implemented.
    private DashboardResponse.Range mockRange(LocalDateTime startAt, LocalDateTime endAt) {
        return new DashboardResponse.Range(format(startAt), format(endAt));
    }

    // TODO: Remove after LogService total/caution count aggregation and AnalysisService analyzed count aggregation are implemented.
    private DashboardResponse.Stats mockStats() {
        return new DashboardResponse.Stats(15_234, 312, 9_000);
    }

    // TODO: Remove after LogService interval bucket aggregation is implemented.
    private List<DashboardResponse.TimeSeriesItem> mockTimeSeries(
            LocalDateTime startAt,
            LocalDateTime endAt,
            String interval
    ) {
        Duration bucketSize = parseInterval(interval);
        List<DashboardResponse.TimeSeriesItem> items = new ArrayList<>();
        LocalDateTime bucketAt = startAt;
        int bucketIndex = 0;

        while (!bucketAt.isAfter(endAt) && items.size() < 25) {
            items.add(new DashboardResponse.TimeSeriesItem(
                    format(bucketAt),
                    480 + bucketIndex * 15,
                    9 + bucketIndex
            ));
            bucketAt = bucketAt.plus(bucketSize);
            bucketIndex++;
        }

        if (items.isEmpty()) {
            items.add(new DashboardResponse.TimeSeriesItem(format(endAt), 480, 9));
        }

        return items;
    }

    // TODO: Remove after AnalysisService risk distribution aggregation is implemented.
    private List<DashboardResponse.RiskDistributionItem> mockRiskDistribution() {
        return List.of(
                new DashboardResponse.RiskDistributionItem("긴급", 100),
                new DashboardResponse.RiskDistributionItem("높음", 400),
                new DashboardResponse.RiskDistributionItem("보통", 1_100),
                new DashboardResponse.RiskDistributionItem("낮음", 7_400)
        );
    }

    // TODO: Remove after LogService type distribution aggregation is implemented.
    private List<DashboardResponse.TypeDistributionItem> mockTypeDistribution() {
        return List.of(
                new DashboardResponse.TypeDistributionItem("RAS", 4_200),
                new DashboardResponse.TypeDistributionItem("KERNEL", 1_180)
        );
    }

    // TODO: Remove after LogService component distribution aggregation is implemented.
    private List<DashboardResponse.ComponentDistributionItem> mockComponentDistribution() {
        return List.of(
                new DashboardResponse.ComponentDistributionItem("KERNEL", 8_800),
                new DashboardResponse.ComponentDistributionItem("MMCS", 950)
        );
    }

    // TODO: Remove after LogService level distribution aggregation is implemented.
    private List<DashboardResponse.LevelDistributionItem> mockLevelDistribution() {
        return List.of(
                new DashboardResponse.LevelDistributionItem("INFO", 14_000),
                new DashboardResponse.LevelDistributionItem("WARNING", 780),
                new DashboardResponse.LevelDistributionItem("ERROR", 312),
                new DashboardResponse.LevelDistributionItem("FATAL", 120),
                new DashboardResponse.LevelDistributionItem("SEVERE", 18),
                new DashboardResponse.LevelDistributionItem("FAILURE", 6)
        );

    }

    // TODO: Remove after LogService recent caution log query is implemented.
    private List<DashboardResponse.RecentCautionLog> mockRecentCautionLogs(LocalDateTime endAt) {
        return List.of(
                new DashboardResponse.RecentCautionLog(
                        1001L,
                        format(endAt.minusMinutes(9)),
                        "R02-M1-N0-C:J12-U11",
                        "KERNEL",
                        "FATAL",
                        "RAS",
                        "KERNDTLB",
                        true,
                        true,
                        "data TLB error interrupt"
                ),
                new DashboardResponse.RecentCautionLog(
                        1002L,
                        format(endAt.minusMinutes(3)),
                        "R03-M0-N1-C:J04-U03",
                        "KERNEL",
                        "ERROR",
                        "RAS",
                        "APPREAD",
                        true,
                        false,
                        "application read error detected"
                )
        );
    }

    // TODO: Remove after PatternService recent pattern query is implemented.
    private List<DashboardResponse.RecentPattern> mockRecentPatterns() {
        return List.of(
                new DashboardResponse.RecentPattern(12L, "Data TLB Error", 87, "높음", 90),
                new DashboardResponse.RecentPattern(13L, "Application Read Error", 42, "보통", 72)
        );
    }

    private Duration parseInterval(String interval) {
        // TODO: Replace this mock parser with API-level interval validation when real LogService bucketing is connected.
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
            log.info("[Dashboard] unsupported interval value for mock timeSeries, fallback to 1h. interval={}", interval);
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
        log.info("[Dashboard] non-positive interval value for mock timeSeries, fallback to 1h.");
        return Duration.ofHours(1);
    }

    private String format(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}
