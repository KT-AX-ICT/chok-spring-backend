package com.sesac.chok.domain.log.service;

import com.sesac.chok.domain.log.entity.BglLog;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 시연용 BGL CSV(헤더 {@code LineId,Label,Timestamp,Date,Node,Time,NodeRepeat,Type,Component,Level,Content})를
 * {@link BglLog}로 변환한다. Content에 콤마가 포함된 행은 따옴표로 quoting되어 있어 RFC-4180 방식으로 파싱한다.
 * <p>정상/이상 분류는 {@code Label} 컬럼(첫 라벨)을 그대로 보존하며 AI 추론을 하지 않는다.
 */
@Component
public class BglLogCsvParser {

    /** 예: {@code 2026-06-22-00.09.21.588637} (마이크로초 6자리). */
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private static final String FATAL = "FATAL";

    private static final int COL_LABEL = 1;
    private static final int COL_NODE = 4;
    private static final int COL_TIME = 5;
    private static final int COL_NODE_REPEAT = 6;
    private static final int COL_TYPE = 7;
    private static final int COL_COMPONENT = 8;
    private static final int COL_LEVEL = 9;
    private static final int COL_CONTENT = 10;
    private static final int COLUMN_COUNT = 11;

    public List<BglLog> parse(Reader reader) {
        List<BglLog> logs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(reader)) {
            br.readLine(); // 헤더 스킵
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                logs.add(toBglLog(splitCsv(line)));
            }
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("BGL seed CSV 읽기 실패", e);
        }
        return logs;
    }

    private BglLog toBglLog(String[] f) {
        String level = f[COL_LEVEL];
        return BglLog.builder()
                .label(f[COL_LABEL])
                .node(f[COL_NODE])
                .occurredAt(LocalDateTime.parse(f[COL_TIME], TIME_FORMAT))
                .nodeRepeat(f[COL_NODE_REPEAT])
                .logType(f[COL_TYPE])
                .component(f[COL_COMPONENT])
                .logLevel(level)
                .content(f[COL_CONTENT])
                .isFatal(FATAL.equals(level))
                .build();
    }

    /** RFC-4180 최소 구현: 따옴표로 감싼 필드의 콤마/이스케이프 따옴표("")를 처리한다. */
    private String[] splitCsv(String line) {
        String[] fields = new String[COLUMN_COUNT];
        StringBuilder field = new StringBuilder();
        int index = 0;
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields[index++] = field.toString();
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields[index] = field.toString();
        return fields;
    }
}
