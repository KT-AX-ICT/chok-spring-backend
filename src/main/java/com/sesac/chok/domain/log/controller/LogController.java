package com.sesac.chok.domain.log.controller;

import com.sesac.chok.domain.log.dto.LogDetailDto;
import com.sesac.chok.domain.log.dto.LogSearchCondition;
import com.sesac.chok.domain.log.dto.LogSummary;
import com.sesac.chok.domain.log.service.LogService;
import com.sesac.chok.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그 목록·상세(`GET /logs`, `GET /logs/{logId}`, §3.2·§3.3).
 * base path({@code /api/v1})는 {@code WebMvcConfig}가 일괄 적용한다.
 */
@RestController
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping("/logs")
    public PageResponse<LogSummary> getLogs(
            @ModelAttribute LogSearchCondition condition,
            @PageableDefault(size = 50, sort = "occurredAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return logService.getLogs(condition, pageable);
    }

    @GetMapping("/logs/{logId}")
    public LogDetailDto getLogDetail(@PathVariable Long logId) {
        return logService.getLogDetail(logId);
    }
}
