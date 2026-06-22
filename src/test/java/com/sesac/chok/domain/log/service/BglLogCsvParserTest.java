package com.sesac.chok.domain.log.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sesac.chok.domain.log.entity.BglLog;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class BglLogCsvParserTest {

    private final BglLogCsvParser parser = new BglLogCsvParser();

    private static final String HEADER =
            "LineId,Label,Timestamp,Date,Node,Time,NodeRepeat,Type,Component,Level,Content";

    @Test
    void skipsHeaderAndParsesEachDataRow() {
        String csv = HEADER + "\n"
                + "1,-,1782086961,2026.06.22,R02-M1-N0-C:J12-U11,2026-06-22-00.09.21.588637,"
                + "R02-M1-N0-C:J12-U11,RAS,KERNEL,INFO,instruction cache parity error corrected\n"
                + "2,-,1782088560,2026.06.22,R02-M1-N0-C:J12-U11,2026-06-22-00.36.00.565158,"
                + "R02-M1-N0-C:J12-U11,RAS,KERNEL,INFO,instruction cache parity error corrected\n";

        List<BglLog> logs = parser.parse(new StringReader(csv));

        assertThat(logs).hasSize(2);
    }

    @Test
    void mapsColumnsToFields() {
        String csv = HEADER + "\n"
                + "1,-,1782086961,2026.06.22,R02-M1-N0-C:J12-U11,2026-06-22-00.09.21.588637,"
                + "R02-M1-N0-C:J12-U11,RAS,KERNEL,INFO,instruction cache parity error corrected\n";

        BglLog log = parser.parse(new StringReader(csv)).get(0);

        assertThat(log.getLabel()).isEqualTo("-");
        assertThat(log.getNode()).isEqualTo("R02-M1-N0-C:J12-U11");
        assertThat(log.getNodeRepeat()).isEqualTo("R02-M1-N0-C:J12-U11");
        assertThat(log.getLogType()).isEqualTo("RAS");
        assertThat(log.getComponent()).isEqualTo("KERNEL");
        assertThat(log.getLogLevel()).isEqualTo("INFO");
        assertThat(log.getContent()).isEqualTo("instruction cache parity error corrected");
        assertThat(log.getOccurredAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 22, 0, 9, 21, 588637000));
        assertThat(log.getEventId()).isNull();
    }

    @Test
    void marksFatalLevelRowAsIsFatal() {
        String csv = HEADER + "\n"
                + "1573,KERNDTLB,1782407412,2026.06.25,R30-M0-N9-C:J16-U01,2026-06-25-17.10.12.715707,"
                + "R30-M0-N9-C:J16-U01,RAS,KERNEL,FATAL,data TLB error interrupt\n";

        BglLog log = parser.parse(new StringReader(csv)).get(0);

        assertThat(log.isFatal()).isTrue();
        assertThat(log.getLabel()).isEqualTo("KERNDTLB");
    }

    @Test
    void marksNonFatalLevelRowAsNotFatal() {
        String csv = HEADER + "\n"
                + "1,-,1782086961,2026.06.22,R02-M1-N0-C:J12-U11,2026-06-22-00.09.21.588637,"
                + "R02-M1-N0-C:J12-U11,RAS,KERNEL,INFO,instruction cache parity error corrected\n";

        BglLog log = parser.parse(new StringReader(csv)).get(0);

        assertThat(log.isFatal()).isFalse();
    }

    @Test
    void preservesCommaInsideQuotedContent() {
        String csv = HEADER + "\n"
                + "44,-,1782133482,2026.06.22,R16-M1-N2-C:J17-U01,2026-06-22-13.04.42.787352,"
                + "R16-M1-N2-C:J17-U01,RAS,KERNEL,INFO,\"CE sym 2, at 0x0b85eee0, mask 0x05\"\n";

        BglLog log = parser.parse(new StringReader(csv)).get(0);

        assertThat(log.getContent()).isEqualTo("CE sym 2, at 0x0b85eee0, mask 0x05");
    }

    @Test
    void ignoresBlankLines() {
        String csv = HEADER + "\n"
                + "1,-,1782086961,2026.06.22,R02-M1-N0-C:J12-U11,2026-06-22-00.09.21.588637,"
                + "R02-M1-N0-C:J12-U11,RAS,KERNEL,INFO,instruction cache parity error corrected\n"
                + "\n";

        List<BglLog> logs = parser.parse(new StringReader(csv));

        assertThat(logs).hasSize(1);
    }
}
