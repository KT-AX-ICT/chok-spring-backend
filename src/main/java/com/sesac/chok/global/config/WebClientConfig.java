package com.sesac.chok.global.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient pythonWebClient(
            @Value("${python.base-url}") String baseUrl,
            @Value("${python.response-timeout-seconds:310}") long responseTimeoutSeconds) {
        // read(응답) 타임아웃은 FastAPI 자체 배치 캡(batch_timeout_s=300s)보다 살짝 길게 둔다(기본 310s).
        // 그래야 FastAPI가 결과를 주거나 자기 503을 돌려줄 때까지 기다리는 안전망이 되고,
        // 다 끝나가는 배치를 Spring이 먼저 끊어 502(전송 끊김)로 만드는 경합을 피한다.
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(Duration.ofSeconds(responseTimeoutSeconds));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
