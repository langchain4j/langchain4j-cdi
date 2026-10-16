/** */
package dev.langchain4j.cdi.telemetry;

import dev.langchain4j.cdi.telemetry.conventions.GenAIMetrics;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Base class providing OpenTelemetry metrics attribute population for GenAI semantic conventions.
 *
 * @author Buhake Sindi
 * @since 14 October 2026
 */
public abstract class GenAIMetricsTelemetry {

    /** Creates a new instance. */
    protected GenAIMetricsTelemetry() {}

    /** Logger for subclasses. */
    protected final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    /** Attribute key for the token type ({@code gen_ai.token.type}). */
    protected static final AttributeKey<String> TOKEN_TYPE = AttributeKey.stringKey("gen_ai.token.type");

    private static final String METRIC_CLIENT_TOKEN_USAGE_NAME = "gen_ai.client.token.usage";

    private LongHistogram clientTokenUsage;
    private DoubleHistogram clientOperationDuration;

    @Inject
    private Meter meter;

    @PostConstruct
    private void init() {
        clientTokenUsage = meter.histogramBuilder(METRIC_CLIENT_TOKEN_USAGE_NAME)
                .ofLongs()
                .setDescription("Measures number of input and output tokens used")
                .setExplicitBucketBoundariesAdvice(List.of(
                        1L, 4L, 16L, 64L, 256L, 1024L, 4096L, 16384L, 65536L, 262144L, 1048576L, 4194304L, 16777216L,
                        67108864L))
                .build();

        clientOperationDuration = meter.histogramBuilder(GenAIMetrics.CLIENT_OPERATION_DURATION.toString())
                .setDescription("GenAI operation duration")
                .setExplicitBucketBoundariesAdvice(
                        List.of(0.01, 0.02, 0.04, 0.08, 0.16, 0.32, 0.64, 1.28, 2.56, 5.12, 10.24, 20.48, 40.96, 81.92))
                .setUnit("s")
                .build();
    }

    /**
     * Records a client token usage metric.
     *
     * @param value the token count
     * @param attributes the metric attributes
     */
    protected void recordClientTokenUsageMetrics(final long value, final Attributes attributes) {
        clientTokenUsage.record(value, attributes);
    }

    /**
     * Records the client operation duration metric.
     *
     * @param startTimeNanos the operation start time in nanoseconds
     * @param endTimeNanos the operation end time in nanoseconds
     * @param attributes the metric attributes
     */
    protected void recordClientOperationDurationMetrics(
            final long startTimeNanos, final long endTimeNanos, final Attributes attributes) {
        // Use TimeUnit to get the conversion factor dynamically, then divide using double literals
        double seconds = (double) (endTimeNanos - startTimeNanos) / TimeUnit.SECONDS.toNanos(1);
        clientOperationDuration.record(seconds, attributes);
    }
}
