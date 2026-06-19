package com.sesac.chok.domain.log.repository;

import com.sesac.chok.domain.log.entity.BglLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BglLogRepository extends JpaRepository<BglLog, Long> {
}
