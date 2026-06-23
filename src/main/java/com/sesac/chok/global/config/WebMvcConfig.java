package com.sesac.chok.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 모든 도메인 REST 컨트롤러에 {@code /api/v1} base path를 일괄 적용한다.
 * <p>{@code server.servlet.context-path} 전역 방식 대신 {@link PathMatchConfigurer#addPathPrefix}로
 * {@code com.sesac.chok} 패키지의 {@code @Controller}에만 prefix를 붙인다. 따라서 h2-console·actuator
 * 같은 인프라 엔드포인트는 루트 경로에 그대로 남고, 앞으로 추가되는 도메인 컨트롤러는 자동으로 prefix가 적용된다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    public static final String API_BASE_PATH = "/api/v1";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                API_BASE_PATH, HandlerTypePredicate.forBasePackage("com.sesac.chok"));
    }
}
