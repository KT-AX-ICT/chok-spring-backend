package com.sesac.chok.domain.log.service;

import com.sesac.chok.domain.log.dto.LogSearchCondition;
import com.sesac.chok.domain.log.dto.LogSummary;
import com.sesac.chok.domain.log.repository.BglLogRepository;
import com.sesac.chok.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그 목록 조회(`GET /logs`). 다중 nullable 필터 + 페이지네이션을 repository 동적 쿼리에 위임한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogService {

    private final BglLogRepository bglLogRepository;

    public PageResponse<LogSummary> getLogs(LogSearchCondition cond, Pageable pageable) {
        return PageResponse.of(bglLogRepository.searchLogs(
                cond.startAt(), cond.endAt(), cond.riskLevel(), cond.logType(), cond.component(),
                cond.logLevel(), cond.label(), cond.keyword(), cond.isCaution(), cond.isAnalysis(),
                pageable));
    }
}
