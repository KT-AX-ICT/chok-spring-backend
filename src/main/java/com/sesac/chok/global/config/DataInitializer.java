package com.sesac.chok.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        log.info("Data initializer started.");

        // TODO 이예지님의 LogSeedService.initializeIfEmpty()가 구현되면 이 클래스에서 호출하도록 연결
        // TODO seed data가 없을 때만 raw-data/BGL_2k.log를 파싱/적재
        // TODO DataInitializer는 트리거만 담당하고 Repository에 직접 접근하지 않는다
        // 정상/이상 분류는 BGL 첫 번째 라벨 컬럼 기준이다.
        log.info("Data initializer skipped. Log seed loading is not implemented yet.");
    }
}
