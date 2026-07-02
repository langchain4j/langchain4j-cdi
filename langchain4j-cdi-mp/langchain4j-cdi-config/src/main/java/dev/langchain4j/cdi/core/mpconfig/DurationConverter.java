/** MicroProfile Config integration for LangChain4j CDI. */
package dev.langchain4j.cdi.core.mpconfig;

import java.time.Duration;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * MicroProfile Config converter that parses duration strings into {@link Duration} instances.
 *
 * <p>Accepts ISO-8601 duration format (e.g. {@code PT30S}, {@code PT5M}) as well as shorthand notation without the
 * {@code PT} prefix (e.g. {@code 30S}, {@code 5M}).
 *
 * @author Buhake Sindi
 * @since 09 July 2025
 */
public class DurationConverter implements Converter<Duration> {

    /** Creates a new {@code DurationConverter}. */
    public DurationConverter() {}

    @Override
    public Duration convert(String value) throws IllegalArgumentException, NullPointerException {
        // TODO Auto-generated method stub
        String durationString = value.toUpperCase();
        if (!durationString.startsWith("PT")) durationString = "PT" + durationString;
        return Duration.parse(durationString);
    }
}
