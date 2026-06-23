package com.sesac.chok.global.config;

import com.sesac.chok.domain.log.service.BglTemplateSeedService;
import com.sesac.chok.domain.log.service.LogSeedService;
import com.sesac.chok.domain.pattern.service.PatternViewSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 seed 초기화를 트리거하는 역할만 담당한다(Repository 직접 접근 없음).
 * <p>정본 카탈로그({@code bgl_template}=이벤트 템플릿, {@code pattern_view}=반복 패턴)와 시연용
 * {@code bgl_log}를 각 SeedService에 위임해 비어 있을 때만 적재한다. {@code bgl_log}의 정상/이상 판정
 * ({@code is_abnormal})은 적재 시 null(미분석)이고 2차 FastAPI 분석 결과로 추후 채워진다.
 * <p>{@code app.seed.enabled=false}면 비활성 — 테스트는 {@code src/test/resources/application.properties}에서
 * 꺼서 seed가 모든 {@code @SpringBootTest} 컨텍스트를 오염시키지 않도록 한다. 미설정 시 기본 적재(실서비스 기동).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements ApplicationRunner {

    private final BglTemplateSeedService bglTemplateSeedService;
    private final PatternViewSeedService patternViewSeedService;
    private final LogSeedService logSeedService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Data initializer started.");
        bglTemplateSeedService.initializeIfEmpty(); // 이벤트 템플릿 카탈로그(정본)
        patternViewSeedService.initializeIfEmpty(); // 반복 패턴(정본)
        logSeedService.initializeIfEmpty();         // 시연용 BGL 로그
    }
}
