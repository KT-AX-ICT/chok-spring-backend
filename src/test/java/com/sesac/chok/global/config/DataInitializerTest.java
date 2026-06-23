package com.sesac.chok.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sesac.chok.domain.log.service.BglTemplateSeedService;
import com.sesac.chok.domain.log.service.LogSeedService;
import com.sesac.chok.domain.pattern.service.PatternViewSeedService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.stereotype.Component;

class DataInitializerTest {

    @Test
    void dataInitializerIsApplicationRunnerComponent() {
        assertThat(DataInitializer.class)
                .isAssignableTo(ApplicationRunner.class)
                .hasAnnotation(Component.class);
    }

    @Test
    void runDelegatesEachSeedLoadingToItsService() {
        BglTemplateSeedService templateSeed = mock(BglTemplateSeedService.class);
        PatternViewSeedService patternSeed = mock(PatternViewSeedService.class);
        LogSeedService logSeed = mock(LogSeedService.class);
        DataInitializer dataInitializer = new DataInitializer(templateSeed, patternSeed, logSeed);
        ApplicationArguments args = new DefaultApplicationArguments();

        assertThatNoException().isThrownBy(() -> dataInitializer.run(args));

        verify(templateSeed).initializeIfEmpty();
        verify(patternSeed).initializeIfEmpty();
        verify(logSeed).initializeIfEmpty();
    }
}
