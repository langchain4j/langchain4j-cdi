/** MicroProfile Config integration for LangChain4j CDI. */
package dev.langchain4j.cdi.core.mpconfig;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * MicroProfile Config converter that parses duration strings into {@link Duration} instances.
 *
 * <p>Accepts ISO-8601 durations (e.g. {@code PT30S}, {@code P1D}, {@code P1DT2H}) as well as a shorthand without the
 * {@code P}/{@code PT} prefix (e.g. {@code 30s}, {@code 1h30m}, {@code 1d}, {@code 1d2h}). Values are case-insensitive
 * and surrounding whitespace is ignored. A blank value is treated as empty and converts to {@code null}.
 *
 * @author Buhake Sindi
 * @since 09 July 2025
 */
public class DurationConverter implements Converter<Duration> {

    /** Creates a new {@code DurationConverter}. */
    public DurationConverter() {}

    @Override
    public Duration convert(String value) throws IllegalArgumentException, NullPointerException {
        Objects.requireNonNull(value, "value must not be null");
        String duration = value.trim().toUpperCase(Locale.ROOT);
        if (duration.isEmpty()) {
            return null;
        }
        try {
            return Duration.parse(toIso8601(duration));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid duration: '" + value + "'", e);
        }
    }

    /** Adds the ISO-8601 prefix to a shorthand value; full ISO-8601 values are returned unchanged. */
    private static String toIso8601(String duration) {
        String unsigned = duration.startsWith("-") || duration.startsWith("+") ? duration.substring(1) : duration;
        if (unsigned.startsWith("P")) {
            return duration;
        }
        int days = duration.indexOf('D');
        if (days < 0) {
            return "PT" + duration;
        }
        String time = duration.substring(days + 1);
        return "P" + duration.substring(0, days + 1) + (time.isEmpty() ? "" : "T" + time);
    }
}
