package dev.langchain4j.cdi.core.mpconfig;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DurationConverterTest {

    private final DurationConverter converter = new DurationConverter();

    @ParameterizedTest
    @CsvSource({
        // full ISO-8601, accepted as-is
        "PT30S, PT30S",
        "pt5m, PT5M",
        "P1D, PT24H",
        "P1DT2H, PT26H",
        "-PT5S, PT-5S",
        // shorthand, alias for PT...
        "5s, PT5S",
        "1h30m, PT1H30M",
        "1.5S, PT1.5S",
        "-5s, PT-5S",
        // shorthand with days
        "1D, PT24H",
        "1d2h, PT26H",
    })
    void convert_validValue_returnsDuration(String value, String expected) {
        assertEquals(Duration.parse(expected), converter.convert(value));
    }

    @Test
    void convert_surroundingWhitespace_isTrimmed() {
        assertEquals(Duration.ofSeconds(5), converter.convert(" 5s "));
        assertEquals(Duration.ofDays(1), converter.convert("\tP1D\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void convert_emptyValue_returnsNull(String value) {
        assertNull(converter.convert(value));
    }

    @Test
    void convert_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> converter.convert(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "5", "PT", "P", "1x", "PTP1D", "5s1d"})
    void convert_invalidValue_throwsIllegalArgumentException(String value) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> converter.convert(value));
        assertTrue(e.getMessage().contains(value), e.getMessage());
    }
}
