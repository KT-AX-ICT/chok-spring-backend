package com.sesac.chok.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ResponsePlanParserTest {

    @Test
    void parsesJsonArray() {
        String action = "[\"노드 격리\", \"메모리 진단\", \"보드 교체 검토\"]";

        List<String> result = ResponsePlanParser.parse(action);

        assertThat(result).containsExactly("노드 격리", "메모리 진단", "보드 교체 검토");
    }

    @Test
    void parsesNewlineSeparated() {
        String action = "노드 격리\n메모리 진단\n보드 교체 검토";

        List<String> result = ResponsePlanParser.parse(action);

        assertThat(result).containsExactly("노드 격리", "메모리 진단", "보드 교체 검토");
    }

    @Test
    void returnsEmptyListForNull() {
        assertThat(ResponsePlanParser.parse(null)).isEmpty();
    }

    @Test
    void returnsEmptyListForBlank() {
        assertThat(ResponsePlanParser.parse("   ")).isEmpty();
    }

    @Test
    void fallsBackToRawWhenJsonMalformed() {
        String action = "[broken json";

        List<String> result = ResponsePlanParser.parse(action);

        assertThat(result).containsExactly("[broken json");
    }

    @Test
    void trimsAndDropsBlankLines() {
        String action = "  첫째 \n\n  둘째  \n";

        List<String> result = ResponsePlanParser.parse(action);

        assertThat(result).containsExactly("첫째", "둘째");
    }
}
