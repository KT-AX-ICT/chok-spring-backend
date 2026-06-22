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

    /**
     * 예: {@code 2026-06-22-00.09.21.588637}.
     * <p><b>주의</b>: {@code SSSSSS}는 마이크로초 <b>정확히 6자리</b> 고정이다. seed CSV를 재생성할 때
     * 분수 자릿수가 달라지면(예: trailing zero 제거, 초 단위) {@code DateTimeParseException}이 난다.
     * 현재 seed는 6자리로 고정이라 그대로 두며, 포맷이 바뀌면 가변 분수(appendFraction)로 전환할 것.
     */
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
            br.readLine(); // 첫 줄(헤더) 스킵. 헤더 없는 CSV가 들어오면 첫 데이터 행이 누락되니 주의.
            String line;
            int lineNumber = 1; // 헤더가 1번 줄
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = splitCsv(line);
                if (fields.size() != COLUMN_COUNT) {
                    // seed CSV는 외부에서 가공해 교체될 수 있으므로, 컬럼 수가 어긋나면
                    // 크래시(AIOOBE/NPE) 대신 줄 번호를 담은 명확한 예외로 실패한다.
                    throw new IllegalStateException(
                            "BGL seed CSV %d번째 줄 컬럼 수 불일치: expected %d, actual %d (라인: %s)"
                                    .formatted(lineNumber, COLUMN_COUNT, fields.size(), line));
                }
                logs.add(toBglLog(fields));
            }
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("BGL seed CSV 읽기 실패", e);
        }
        return logs;
    }

    private BglLog toBglLog(List<String> f) {
        String level = f.get(COL_LEVEL);
        return BglLog.builder()
                .label(f.get(COL_LABEL))
                .node(f.get(COL_NODE))
                .occurredAt(LocalDateTime.parse(f.get(COL_TIME), TIME_FORMAT))
                .nodeRepeat(f.get(COL_NODE_REPEAT))
                .logType(f.get(COL_TYPE))
                .component(f.get(COL_COMPONENT))
                .logLevel(level)
                .content(f.get(COL_CONTENT))
                .isFatal(FATAL.equals(level))
                .build();
    }

    /** RFC-4180 최소 구현: 따옴표로 감싼 필드의 콤마/이스케이프 따옴표("")를 처리한다. */
    private List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
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
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields;
    }
}
