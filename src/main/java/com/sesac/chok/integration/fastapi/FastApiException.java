package com.sesac.chok.integration.fastapi;

import com.sesac.chok.integration.fastapi.dto.FastApiErrorResponse;
import org.springframework.http.HttpStatusCode;

/**
 * 외부 FastAPI 호출 실패를 Spring 예외 흐름으로 변환한 예외.
 * <p>두 갈래를 모두 포괄한다.
 * <ul>
 *   <li><b>upstream</b>: FastAPI가 4xx/5xx로 응답 — {@link #status()}와 {@link #body()}(가능 시) 보유</li>
 *   <li><b>unavailable</b>: 연결 거부·타임아웃 등 응답 자체를 받지 못함 — {@code status}/{@code body}는 {@code null}</li>
 * </ul>
 * 어느 쪽이든 Spring 클라이언트에는 502(Bad Gateway)로 노출한다({@code GlobalExceptionHandler}).
 */
public class FastApiException extends RuntimeException {

    /** FastAPI가 돌려준 상태 코드. 응답을 받지 못한 전송 오류면 {@code null}. */
    private final transient HttpStatusCode status;

    /** FastAPI 에러 본문(파싱 성공 시). 본문이 없거나 전송 오류면 {@code null}. */
    private final transient FastApiErrorResponse body;

    private FastApiException(String message, HttpStatusCode status, FastApiErrorResponse body, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.body = body;
    }

    /** FastAPI가 4xx/5xx로 응답한 경우. */
    public static FastApiException upstream(HttpStatusCode status, FastApiErrorResponse body) {
        String detail = body != null ? body.code() + " - " + body.message() : "no error body";
        return new FastApiException("FastAPI 응답 오류 (" + status + "): " + detail, status, body, null);
    }

    /** 연결 거부·타임아웃 등 응답을 받지 못한 경우. */
    public static FastApiException unavailable(String detail, Throwable cause) {
        return new FastApiException("FastAPI 호출 실패: " + detail, null, null, cause);
    }

    public HttpStatusCode status() {
        return status;
    }

    public FastApiErrorResponse body() {
        return body;
    }
}
