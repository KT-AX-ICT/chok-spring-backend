package com.sesac.chok.domain.dashboard.controller;

import com.sesac.chok.domain.dashboard.dto.DashboardResponse;
import com.sesac.chok.domain.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "대시보드 통합 조회", description = "date가 없으면 서버 현재 시각 기준 최근 24시간 데이터를 조회합니다.")
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @Parameter(
                    description = "조회 종료 시각. 미입력 시 서버 현재 시각을 사용합니다.",
                    example = "2026-06-19T21:00:00"
            )
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @RequestParam(required = false) LocalDateTime date
    ) {
        return ResponseEntity.ok(dashboardService.getDashboard(date));
    }
}
