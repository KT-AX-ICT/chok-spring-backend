package com.sesac.chok.integration.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 배치 항목의 <b>처리 완료 여부</b>. 로그 판정({@code isAbnormal} 정상/이상)과는 별개 개념이다.
 * <p>FastAPI 와이어 값은 소문자({@code "success"}/{@code "fail"})라, 양방향 변환을 명시한다.
 */
public enum ProcessStatus {
    SUCCESS,
    FAIL;

    @JsonValue
    public String wire() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static ProcessStatus from(String value) {
        return valueOf(value.toUpperCase());
    }
}
