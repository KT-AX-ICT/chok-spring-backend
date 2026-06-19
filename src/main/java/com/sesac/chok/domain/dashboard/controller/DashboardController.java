package com.sesac.chok.domain.dashboard.controller;

import com.sesac.chok.domain.dashboard.dto.DashboardResponse;
import com.sesac.chok.domain.dashboard.service.DashboardService;
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

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(@RequestParam(required = false) String date) {
        return ResponseEntity.ok(dashboardService.getDashboard(date));
    }
}
