package com.sesac.chok.domain.dashboard.dto;

import java.util.List;

public record DashboardResponse(
        Range range,
        Stats stats,
        List<TimeSeriesItem> timeSeries,
        List<RiskDistributionItem> riskDistribution,
        List<TypeDistributionItem> typeDistribution,
        List<ComponentDistributionItem> componentDistribution,
        List<LevelDistributionItem> levelDistribution,
        List<RecentCautionLog> recentCautionLogs,
        List<RecentPattern> recentPatterns
) {

    public record Range(String startAt, String endAt) {
    }

    public record Stats(int totalLogCount, int cautionLogCount, int analyzedLogCount) {
    }

    public record TimeSeriesItem(String time, int totalCount, int cautionCount) {
    }

    public record RiskDistributionItem(String riskLevel, int count) {
    }

    public record TypeDistributionItem(String logType, int count) {
    }

    public record ComponentDistributionItem(String component, int count) {
    }

    public record LevelDistributionItem(String logLevel, int count) {
    }

    public record RecentCautionLog(
            long logId,
            String occurredAt,
            String node,
            String component,
            String logLevel,
            boolean isCaution,
            boolean isAnalysis,
            String content
    ) {
    }

    public record RecentPattern(long patternId, String patternName, int count, int importance) {
    }
}
