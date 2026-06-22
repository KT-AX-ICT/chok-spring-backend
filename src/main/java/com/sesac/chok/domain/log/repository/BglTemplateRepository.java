package com.sesac.chok.domain.log.repository;

import com.sesac.chok.domain.log.entity.BglTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code bgl_template} 리포지토리. PK는 자연키 {@code event_id}(String).
 */
public interface BglTemplateRepository extends JpaRepository<BglTemplate, String> {
}
