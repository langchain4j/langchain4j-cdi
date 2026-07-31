package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.transport.McpProgressReporter;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.mcpjava.server.progress.ProgressToken;
import org.mcpjava.server.progress.ProgressTracker;

/** Thread-safe implementation of {@link ProgressTracker} that delegates to {@link McpProgressReporter}. */
public class CdiProgressTracker implements ProgressTracker {

    private final Object rawToken;
    private final BigDecimal totalValue;
    private final BigDecimal stepValue;
    private final Function<BigDecimal, String> messageBuilder;
    private final McpProgressReporter progressReporter;
    private final AtomicReference<BigDecimal> currentProgress;

    /**
     * @param rawToken the raw progress token from the request, or {@code null} if none
     * @param totalValue the optional total value, or {@code null} if unknown
     * @param stepValue the default step size for each {@link #advanceAndForget()} call
     * @param messageBuilder optional function that produces a status message from the current progress
     * @param progressReporter the reporter that sends progress notifications over the wire
     */
    CdiProgressTracker(
            Object rawToken,
            BigDecimal totalValue,
            BigDecimal stepValue,
            Function<BigDecimal, String> messageBuilder,
            McpProgressReporter progressReporter) {
        this.rawToken = rawToken;
        this.totalValue = totalValue;
        this.stepValue = stepValue;
        this.messageBuilder = messageBuilder;
        this.progressReporter = progressReporter;
        this.currentProgress = new AtomicReference<>(BigDecimal.ZERO);
    }

    @Override
    public ProgressToken token() {
        return rawToken != null ? CdiProgressToken.of(rawToken) : null;
    }

    @Override
    public void advanceAndForget(BigDecimal amount) {
        BigDecimal newProgress = currentProgress.accumulateAndGet(amount, BigDecimal::add);
        if (rawToken != null && progressReporter != null) {
            String message = messageBuilder != null ? messageBuilder.apply(newProgress) : null;
            progressReporter.reportProgress(
                    rawToken, newProgress.doubleValue(), totalValue != null ? totalValue.doubleValue() : 0, message);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T advance(BigDecimal amount) {
        advanceAndForget(amount);
        return (T) null;
    }

    // advanceAndForget() / advanceAndForget(long) / advanceAndForget(double) have default implementations in the
    // interface

    @Override
    public BigDecimal progress() {
        return currentProgress.get();
    }

    @Override
    public Optional<BigDecimal> total() {
        return Optional.ofNullable(totalValue);
    }

    @Override
    public BigDecimal step() {
        return stepValue;
    }

    static class CdiBuilder implements ProgressTracker.Builder {

        private final Object rawToken;
        private final McpProgressReporter progressReporter;
        private BigDecimal totalValue = null;
        private BigDecimal stepValue = BigDecimal.ONE;
        private Function<BigDecimal, String> messageBuilder = null;

        CdiBuilder(Object rawToken, McpProgressReporter progressReporter) {
            this.rawToken = rawToken;
            this.progressReporter = progressReporter;
        }

        @Override
        public Builder setTotal(long total) {
            this.totalValue = BigDecimal.valueOf(total);
            return this;
        }

        @Override
        public Builder setTotal(double total) {
            this.totalValue = BigDecimal.valueOf(total);
            return this;
        }

        @Override
        public Builder setDefaultStep(long step) {
            this.stepValue = BigDecimal.valueOf(step);
            return this;
        }

        @Override
        public Builder setDefaultStep(double step) {
            this.stepValue = BigDecimal.valueOf(step);
            return this;
        }

        @Override
        public Builder setMessageBuilder(Function<BigDecimal, String> messageBuilder) {
            this.messageBuilder = messageBuilder;
            return this;
        }

        @Override
        public ProgressTracker build() {
            return new CdiProgressTracker(rawToken, totalValue, stepValue, messageBuilder, progressReporter);
        }
    }
}
