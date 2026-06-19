package com.sesac.chok.domain.dashboard.controller;

import com.sesac.chok.domain.dashboard.dto.DashboardResponse;
import com.sesac.chok.domain.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "대시보드 통합 조회", description = "대시보드 화면에 필요한 로그/분석 mock 집계 데이터를 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "대시보드 mock 응답",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DashboardResponse.class),
                    examples = @ExampleObject(
                            name = "dashboard-mock-2026-06-19",
                            value = """
                                    {
                                      "range": {
                                        "startAt": "2026-06-19T00:00:00",
                                        "endAt": "2026-06-19T23:59:59"
                                      },
                                      "stats": {
                                        "totalLogCount": 15234,
                                        "cautionLogCount": 312,
                                        "analyzedLogCount": 9000
                                      },
                                      "timeSeries": [
                                        { "time": "2026-06-19T06:00:00", "totalCount": 480, "cautionCount": 9 },
                                        { "time": "2026-06-19T07:00:00", "totalCount": 515, "cautionCount": 10 },
                                        { "time": "2026-06-19T08:00:00", "totalCount": 540, "cautionCount": 12 }
                                      ],
                                      "riskDistribution": [
                                        { "riskLevel": "LOW", "count": 7400 },
                                        { "riskLevel": "MEDIUM", "count": 1100 },
                                        { "riskLevel": "HIGH", "count": 400 },
                                        { "riskLevel": "CRITICAL", "count": 100 }
                                      ],
                                      "typeDistribution": [
                                        { "logType": "RAS", "count": 4200 },
                                        { "logType": "KERNEL", "count": 1180 }
                                      ],
                                      "componentDistribution": [
                                        { "component": "KERNEL", "count": 8800 },
                                        { "component": "MMCS", "count": 950 }
                                      ],
                                      "levelDistribution": [
                                        { "logLevel": "INFO", "count": 14000 },
                                        { "logLevel": "FATAL", "count": 120 }
                                      ],
                                      "recentCautionLogs": [
                                        {
                                          "logId": 1001,
                                          "occurredAt": "2026-06-19T08:51:00",
                                          "node": "R02-M1-N0-C:J12-U11",
                                          "component": "KERNEL",
                                          "logLevel": "FATAL",
                                          "logType": "RAS",
                                          "label": "KERNDTLB",
                                          "isCaution": true,
                                          "isAnalysis": true,
                                          "content": "data TLB error interrupt"
                                        }
                                      ],
                                      "recentPatterns": [
                                        { "patternId": 12, "patternName": "Data TLB Error", "count": 87, "riskLevel": "HIGH", "importance": 90 }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(@RequestParam(required = false) String date) {
        return ResponseEntity.ok(dashboardService.getDashboard(date));
    }
}
