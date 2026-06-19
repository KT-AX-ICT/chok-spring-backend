package com.sesac.chok.domain.dashboard.service;

import com.sesac.chok.domain.dashboard.dto.DashboardResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DashboardService {

    public DashboardResponse getDashboard(String date) {
        log.info("[Dashboard] returning mock response - real aggregation pending, date={}", date);

        // TODO: Replace mock data after LogService/AnalysisService/PatternService dashboard query methods are implemented.
        return new DashboardResponse(
                mockRange(),
                mockStats(),
                mockTimeSeries(),
                mockRiskDistribution(),
                mockTypeDistribution(),
                mockComponentDistribution(),
                mockLevelDistribution(),
                mockRecentCautionLogs(),
                mockRecentPatterns()
        );
    }

    // TODO: Replace mock data after LogService/AnalysisService/PatternService dashboard query methods are implemented.
    private DashboardResponse.Range mockRange() {
        return new DashboardResponse.Range("2026-06-19T00:00:00", "2026-06-19T23:59:59");
    }

    // TODO: Replace mock data after LogService/AnalysisService/PatternService dashboard query methods are implemented.
    private DashboardResponse.Stats mockStats() {
        return new DashboardResponse.Stats(15_234, 312, 9_000);
    }

    // TODO: Replace mock data after LogService/AnalysisService/PatternService dashboard query methods are implemented.
    private List<DashboardResponse.TimeSeriesItem> mockTimeSeries() {
        return List.of(
                new DashboardResponse.TimeSeriesItem("2026-06-19T06:00:00", 480, 9),
                new DashboardResponse.TimeSeriesItem("2026-06-19T07:00:00", 515, 10),
                new DashboardResponse.TimeSeriesItem("2026-06-19T08:00:00", 540, 12)
        );
    }

    // TODO: Replace mock data after LogService/AnalysisService/PatternService dashboard query methods are implemented.
    private List<DashboardResponse.RiskDistributionItem> mockRiskDistribution() {
        return List.of(
                new DashboardResponse.RiskDistributionItem("LOW", 7_400),
                new DashboardResponse.RiskDistributionItem("MEDIUM", 1_100),
                new DashboardResponse.RiskDistributionItem("HIGH", 400),
                new DashboardResponse.RiskDistributionItem("CRITICAL", 100)
        );
    }

    // TODO: Replace mock data after LogService/AnalysisService/PatternService dashboard query methods are implemented.
    private List<DashboardResponse.TypeDistributionItem> mockTypeDistribution() {
        return List.of(
                new DashboardResponse.TypeDistributionItem("RAS", 4_200),
                new DashboardResponse.TypeDistributionItem("KERNEL", 1_180)
        );
    }
    // TODO: Replace mock data after LogService/AnalysisService/PatternService dashboard query methods are implemented.
    private List<DashboardResponse.ComponentDistributionItem> mockComponentDistribution() {
        return List.of(
                new DashboardResponse.ComponentDistributionItem("KERNEL", 8_800),
                new DashboardResponse.ComponentDistributionItem("MMCS", 950)
        );
    }

    // TODO: Replace mock data after LogService/AnalysisService/PatternService dashboard query methods are implemented.
    private List<DashboardResponse.LevelDistributionItem> mockLevelDistribution() {
        return List.of(
                new DashboardResponse.LevelDistributionItem("INFO", 14_000),
                new DashboardResponse.LevelDistributionItem("FATAL", 120)
        );

    }

    // TODO: Replace mock data after LogService/AnalysisService/PatternService dashboard query methods are implemented.
    private List<DashboardResponse.RecentCautionLog> mockRecentCautionLogs() {
        return List.of(
                new DashboardResponse.RecentCautionLog(
                        1001L,
                        "2026-06-19T08:51:00",
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
                        "2026-06-19T08:57:00",
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

    // TODO: Replace mock data after LogService/AnalysisService/PatternService dashboard query methods are implemented.
    private List<DashboardResponse.RecentPattern> mockRecentPatterns() {
        return List.of(
                new DashboardResponse.RecentPattern(12L, "Data TLB Error", 87, "HIGH", 90),
                new DashboardResponse.RecentPattern(13L, "Application Read Error", 42, "MEDIUM", 72)
        );
    }
}
