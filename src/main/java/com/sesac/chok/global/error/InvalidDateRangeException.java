package com.sesac.chok.global.error;

import java.time.LocalDateTime;

/**
 * 날짜 범위 파라미터가 역전된 경우(startAt > endAt). 400 INVALID_DATE_RANGE로 매핑된다.
 * 동적 필터({@code (:p IS NULL OR ...)})는 역전을 빈 결과로만 처리하므로, 진입부에서 명시적으로 막는다.
 */
public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException(String message) {
        super(message);
    }

    /** 둘 다 지정되고 {@code startAt > endAt}이면 예외. 한쪽이라도 null이면 통과(전체 기간 의미). */
    public static void check(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            throw new InvalidDateRangeException("startAt must be on or before endAt: " + startAt + " > " + endAt);
        }
    }
}
