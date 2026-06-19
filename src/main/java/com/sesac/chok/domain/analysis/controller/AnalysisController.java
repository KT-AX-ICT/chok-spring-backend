package com.sesac.chok.domain.analysis.controller;

import com.sesac.chok.domain.analysis.dto.AnalysisDto;
import com.sesac.chok.domain.analysis.service.AnalysisService;
import com.sesac.chok.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/analysis")
    public PageResponse<AnalysisDto> getAnalysisList(
            @PageableDefault(size = 50, sort = "analyzedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return analysisService.getAnalysisList(pageable);
    }
}
