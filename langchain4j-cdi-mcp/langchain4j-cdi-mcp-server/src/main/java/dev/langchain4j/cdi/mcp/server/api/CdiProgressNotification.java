package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.transport.McpProgressReporter;
import java.math.BigDecimal;
import org.mcp_java.server.ProgressNotification;
import org.mcp_java.server.ProgressToken;

/** Implementation of {@link ProgressNotification} that delegates to {@link McpProgressReporter}. */
public class CdiProgressNotification implements ProgressNotification {

    private final Object rawToken;
    private final BigDecimal progressValue;
    private final BigDecimal totalValue;
    private final String message;
    private final McpProgressReporter progressReporter;

    CdiProgressNotification(
            Object rawToken,
            BigDecimal progressValue,
            BigDecimal totalValue,
            String message,
            McpProgressReporter progressReporter) {
        this.rawToken = rawToken;
        this.progressValue = progressValue;
        this.totalValue = totalValue;
        this.message = message;
        this.progressReporter = progressReporter;
    }

    @Override
    public ProgressToken token() {
        return rawToken != null ? new ProgressToken(rawToken) : null;
    }

    @Override
    public BigDecimal total() {
        return totalValue;
    }

    @Override
    public BigDecimal progress() {
        return progressValue;
    }

    @Override
    public String message() {
        return message;
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
        public ProgressNotification build() {
            return new CdiProgressNotification(rawToken, progressValue, totalValue, message, progressReporter);
        }
    }
}
