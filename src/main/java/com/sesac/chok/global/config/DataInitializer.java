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
 * 이 클래스는 Repository에 직접 접근하지 않는다. 정상/이상 분류는 BGL 첫 라벨 컬럼 기준이다.
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
