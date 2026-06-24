package com.sesac.chok.integration.fastapi;

import com.sesac.chok.integration.fastapi.dto.AnalyzeRequest;
import com.sesac.chok.integration.fastapi.dto.AnalyzeResponse;
import com.sesac.chok.integration.fastapi.dto.BatchAnalyzeRequest;
import com.sesac.chok.integration.fastapi.dto.BatchAnalyzeResponse;
import com.sesac.chok.integration.fastapi.dto.FastApiErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

/**
 * FastAPI 분석 endpoint 호출 client. {@code python.base-url} 기반 {@code pythonWebClient}로 호출하고,
 * FastAPI 응답 오류·전송 오류를 모두 {@link FastApiException}으로 변환한다(외부 장애 → Spring 예외 흐름).
 * <p>경계 원칙: Repository에 직접 접근하지 않고, 분석 결과 저장(=domain.analysis)도 수행하지 않는다.
 * 응답 DTO를 그대로 돌려주며, 도메인 저장 모델로의 매핑은 호출 계층(scheduler) 책임이다.
 * <p>스케줄러는 동기 흐름이라 {@code block()}으로 결과를 받는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiClient {

    private static final String ANALYZE_PATH = "/ai/v1/analyze";
    private static final String ANALYZE_BATCH_PATH = "/ai/v1/analyze/batch";

    private final WebClient pythonWebClient;

    /** 다건 분석(스케줄러 기본 경로). */
    public BatchAnalyzeResponse analyzeBatch(BatchAnalyzeRequest request) {
        return post(ANALYZE_BATCH_PATH, request, BatchAnalyzeResponse.class);
    }

    /** 단건 분석(수동/개별 재처리 경로). */
    public AnalyzeResponse analyze(AnalyzeRequest request) {
        return post(ANALYZE_PATH, request, AnalyzeResponse.class);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        try {
            return pythonWebClient.post()
                    .uri(path)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::toException)
                    .bodyToMono(responseType)
                    .block();
        } catch (FastApiException e) {
            throw e; // onStatus에서 만든 응답 오류는 그대로 전파
        } catch (WebClientException e) {
            // 연결 거부·타임아웃 등 응답을 받지 못한 전송 오류
            log.warn("FastAPI 호출 실패 ({}): {}", path, e.getMessage());
            throw FastApiException.unavailable(path, e);
        }
    }

    /** FastAPI 4xx/5xx 응답을 {@link FastApiException}으로 변환한다(에러 본문 파싱 실패에도 견고). */
    private Mono<? extends Throwable> toException(ClientResponse response) {
        HttpStatusCode status = response.statusCode();
        return response.bodyToMono(FastApiErrorResponse.class)
                .map(body -> FastApiException.upstream(status, body))
                .onErrorResume(ignored -> Mono.just(FastApiException.upstream(status, null)))
                .defaultIfEmpty(FastApiException.upstream(status, null));
    }
}
