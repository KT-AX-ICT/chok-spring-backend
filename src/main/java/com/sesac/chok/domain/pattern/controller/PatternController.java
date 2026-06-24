package com.sesac.chok.domain.pattern.controller;

import com.sesac.chok.domain.pattern.dto.PatternDetail;
import com.sesac.chok.domain.pattern.dto.PatternSummary;
import com.sesac.chok.domain.pattern.service.PatternService;
import com.sesac.chok.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 패턴 목록·상세(`GET /log-patterns`, `GET /log-patterns/{patternId}`, §3.5·§3.6).
 * base path({@code /api/v1})는 {@code WebMvcConfig}가 일괄 적용한다.
 */
@RestController
@RequiredArgsConstructor
public class PatternController {

    private final PatternService patternService;

    @GetMapping("/log-patterns")
    public PageResponse<PatternSummary> getPatternList(
            @PageableDefault(size = 20, sort = "importance", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return patternService.getPatternList(pageable);
    }

    @GetMapping("/log-patterns/{patternId}")
    public PatternDetail getPatternDetail(@PathVariable Long patternId) {
        return patternService.getPatternDetail(patternId);
    }
}
