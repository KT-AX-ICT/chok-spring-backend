package com.sesac.chok.domain.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;

/**
 * {@code log_analysis.action}(TEXT) 컬럼을 응답용 {@code responsePlan}(List&lt;String&gt;)로 변환한다.
 * 저장 포맷이 JSON 배열 문자열일 수도, 줄바꿈 구분 텍스트일 수도 있어 두 형태를 모두 방어적으로 처리한다.
 */
public final class ResponsePlanParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private ResponsePlanParser() {
    }

    public static List<String> parse(String action) {
        if (action == null) {
            return List.of();
        }
        String trimmed = action.strip();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        if (trimmed.startsWith("[")) {
            try {
                return clean(MAPPER.readValue(trimmed, STRING_LIST));
            } catch (JsonProcessingException ignored) {
                // JSON 파싱 실패 → 줄바꿈 분할로 폴백
            }
        }
        return clean(Arrays.asList(trimmed.split("\\R")));
    }

    private static List<String> clean(List<String> values) {
        return values.stream()
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
