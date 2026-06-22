package com.sesac.chok.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.sesac.chok.domain.log.service.LogSeedService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.stereotype.Component;

class DataInitializerTest {

    /** 호출 위임만 검증하기 위한 테스트 더블. 실제 파싱/저장은 하지 않는다. */
    private static class RecordingLogSeedService extends LogSeedService {
        private int initializeCount = 0;

        RecordingLogSeedService() {
            super(null, null);
        }

        @Override
        public void initializeIfEmpty() {
            initializeCount++;
        }
    }

    @Test
    void dataInitializerIsApplicationRunnerComponent() {
        assertThat(DataInitializer.class)
                .isAssignableTo(ApplicationRunner.class)
                .hasAnnotation(Component.class);
    }

    @Test
    void runDelegatesSeedLoadingToLogSeedService() {
        RecordingLogSeedService seedService = new RecordingLogSeedService();
        DataInitializer dataInitializer = new DataInitializer(seedService);
        ApplicationArguments args = new DefaultApplicationArguments();

        assertThatNoException().isThrownBy(() -> dataInitializer.run(args));

        assertThat(seedService.initializeCount).isEqualTo(1);
    }
}
