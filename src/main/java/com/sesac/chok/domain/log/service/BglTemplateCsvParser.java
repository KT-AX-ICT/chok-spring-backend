package com.sesac.chok.domain.log.service;

import com.sesac.chok.domain.log.entity.BglTemplate;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 이벤트 템플릿 카탈로그 CSV(헤더 {@code EventId,EventTemplate})를 {@link BglTemplate}로 변환한다.
 * <p>2컬럼 고정이고 {@code EventId}엔 콤마/따옴표가 없으므로 <b>첫 콤마 1회만 분리</b>하고,
 * 나머지({@code EventTemplate})는 따옴표로 감싸였으면 dequote(이스케이프 {@code ""} → {@code "})한다.
 */
@Component
public class BglTemplateCsvParser {

    public List<BglTemplate> parse(Reader reader) {
        List<BglTemplate> templates = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(reader)) {
            br.readLine(); // 헤더(EventId,EventTemplate) 스킵
            String line;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                int comma = line.indexOf(',');
                if (comma < 0) {
                    throw new IllegalStateException(
                            "bgl_template CSV %d번째 줄 컬럼 구분자(,) 없음: %s".formatted(lineNumber, line));
                }
                String eventId = line.substring(0, comma).trim();
                String template = dequote(line.substring(comma + 1));
                templates.add(BglTemplate.builder().eventId(eventId).eventTemplate(template).build());
            }
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("bgl_template CSV 읽기 실패", e);
        }
        return templates;
    }

    /** 따옴표로 감싼 필드면 양끝 따옴표 제거 + 이스케이프 {@code ""} → {@code "} 복원. */
    private String dequote(String field) {
        if (field.length() >= 2 && field.startsWith("\"") && field.endsWith("\"")) {
            return field.substring(1, field.length() - 1).replace("\"\"", "\"");
        }
        return field;
    }
}
