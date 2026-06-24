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

    /**
     * {@code analyzedLogCount}는 분석 완료 전체 수(정상 포함)이고, {@code normalLogCount}는 그 중 정상 판정
     * (riskLevel 없음) 수다. 따라서 {@code analyzedLogCount = riskDistribution 합 + normalLogCount}이며,
     * riskDistribution(긴급/높음/보통/낮음)은 이상 판정만 담는다.
     */
    public record Stats(int totalLogCount, int cautionLogCount, int analyzedLogCount, int normalLogCount) {
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
