package com.sesac.chok.global.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 공통 페이지네이션 응답 포맷. Spring Data {@link Page}를 프론트 계약(`content` + 메타)으로 평탄화한다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
