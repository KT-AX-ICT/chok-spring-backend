package com.sesac.chok.domain.log.repository;

import com.sesac.chok.domain.log.entity.BglLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BglLogRepository extends JpaRepository<BglLog, Long> {

    /**
     * {@code [startAt, endAt)} 범위의 로그를 시각 오름차순으로 조회한다.
     * 대시보드 집계(시간대 버킷팅, 분포, 최근 주의 로그)의 단일 입력 소스다.
     */
    List<BglLog> findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(
            LocalDateTime startAt, LocalDateTime endAt);
}
