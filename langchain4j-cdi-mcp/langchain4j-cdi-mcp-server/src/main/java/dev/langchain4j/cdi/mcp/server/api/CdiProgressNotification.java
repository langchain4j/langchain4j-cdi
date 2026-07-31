package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.transport.McpProgressReporter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.mcpjava.server.progress.ProgressNotification;
import org.mcpjava.server.progress.ProgressToken;

/** Implementation of {@link ProgressNotification} that delegates to {@link McpProgressReporter}. */
public class CdiProgressNotification implements ProgressNotification {

    private final Object rawToken;
    private final BigDecimal progressValue;
    private final BigDecimal totalValue;
    private final String message;
    private final Map<String, Object> metadata;
    private final McpProgressReporter progressReporter;

    /**
     * @param rawToken the raw progress token from the request, or {@code null} if none
     * @param progressValue the current progress value
     * @param totalValue the optional total value, or {@code null} if unknown
     * @param message the optional human-readable status message, or {@code null}
     * @param metadata additional metadata to include in the notification
     * @param progressReporter the reporter that sends the notification over the wire
     */
    CdiProgressNotification(
            Object rawToken,
            BigDecimal progressValue,
            BigDecimal totalValue,
            String message,
            Map<String, Object> metadata,
            McpProgressReporter progressReporter) {
        this.rawToken = rawToken;
        this.progressValue = progressValue;
        this.totalValue = totalValue;
        this.message = message;
        this.metadata = metadata;
        this.progressReporter = progressReporter;
    }

    @Override
    public ProgressToken token() {
        return rawToken != null ? CdiProgressToken.of(rawToken) : null;
    }

    @Override
    public Optional<BigDecimal> total() {
        return Optional.ofNullable(totalValue);
    }

    @Override
    public BigDecimal progress() {
        return progressValue;
    }

    @Override
    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    @Override
    public Map<String, Object> metadata() {
        return metadata;
    }

    @Override
    public void sendAndForget() {
        if (rawToken != null && progressReporter != null) {
            progressReporter.reportProgress(
                    rawToken, progressValue.doubleValue(), totalValue != null ? totalValue.doubleValue() : 0, message);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T send() {
        sendAndForget();
        return (T) null;
    }

    static class CdiBuilder implements ProgressNotification.Builder {

        private final Object rawToken;
        private final McpProgressReporter progressReporter;
        private BigDecimal progressValue = BigDecimal.ZERO;
        private BigDecimal totalValue = null;
        private String message = null;
        private final Map<String, Object> metadata = new HashMap<>();

        CdiBuilder(Object rawToken, McpProgressReporter progressReporter) {
            this.rawToken = rawToken;
            this.progressReporter = progressReporter;
        }

        @Override
        public Builder setProgress(long progress) {
            this.progressValue = BigDecimal.valueOf(progress);
            return this;
        }

        @Override
        public Builder setProgress(double progress) {
            this.progressValue = BigDecimal.valueOf(progress);
            return this;
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
        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Builder putMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        @Override
        public Builder setMetadata(Map<String, Object> metadata) {
            this.metadata.clear();
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }

        @Override
        public ProgressNotification build() {
            return new CdiProgressNotification(
                    rawToken, progressValue, totalValue, message, Map.copyOf(metadata), progressReporter);
        }
    }
}
