package com.sesac.chok.global.config;

import com.sesac.chok.scheduler.AnalysisJobProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 분석 잡 인프라 설정. 5분 스케줄러({@code @Scheduled})·시작 1회 비동기 실행({@code @Async})을 활성화하고
 * 잡 튜너블({@link AnalysisJobProperties})을 바인딩한다. 트리거 자체의 on/off는 각 컴포넌트의
 * {@code @ConditionalOnProperty}가 담당한다(기본 off).
 */
@Configuration
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(AnalysisJobProperties.class)
public class SchedulingConfig {
}
