package com.sesac.chok.global.config;

import com.sesac.chok.domain.log.service.LogSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 seed 초기화를 트리거하는 역할만 담당한다.
 * <p>실제 BGL 파싱/저장과 중복 적재 방지는 {@link LogSeedService}가 처리하며,
 * 이 클래스는 Repository에 직접 접근하지 않는다. {@code label}은 정확도 검증 기준(답지)으로
 * 적재만 하며, 정상/이상 판정({@code is_abnormal})은 적재 시 null(미분석)이고 2차 FastAPI 분석 결과로
 * 추후 채워진다(1차 FATAL은 {@code log_level} 파생값으로 저장하지 않음).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final LogSeedService logSeedService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Data initializer started.");
        logSeedService.initializeIfEmpty();
    }
}
