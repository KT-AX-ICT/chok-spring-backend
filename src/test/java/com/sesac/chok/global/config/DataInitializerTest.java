package com.sesac.chok.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.stereotype.Component;

class DataInitializerTest {

    @Test
    void dataInitializerIsApplicationRunnerComponentWithoutInjectedDependencies() {
        assertThat(DataInitializer.class)
                .isAssignableTo(ApplicationRunner.class)
                .hasAnnotation(Component.class);

        assertThat(DataInitializer.class.getDeclaredConstructors())
                .singleElement()
                .satisfies(constructor -> assertThat(constructor.getParameterCount()).isZero());

        assertThat(DataInitializer.class.getDeclaredFields())
                .filteredOn(field -> !Modifier.isStatic(field.getModifiers()))
                .extracting(Field::getName)
                .isEmpty();
    }

    @Test
    void runLeavesSeedLoadingAsNoOpTrigger() {
        DataInitializer dataInitializer = new DataInitializer();
        ApplicationArguments args = new DefaultApplicationArguments();

        assertThatNoException().isThrownBy(() -> dataInitializer.run(args));
    }
}
