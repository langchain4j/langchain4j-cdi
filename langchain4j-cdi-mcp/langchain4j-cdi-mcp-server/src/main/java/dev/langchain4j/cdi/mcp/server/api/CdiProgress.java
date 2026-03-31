package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.transport.McpProgressReporter;
import java.util.Optional;
import org.mcp_java.server.Progress;
import org.mcp_java.server.ProgressNotification;
import org.mcp_java.server.ProgressToken;
import org.mcp_java.server.ProgressTracker;

/** Implementation of {@link Progress} that wraps a progress token and delegates to {@link McpProgressReporter}. */
public class CdiProgress implements Progress {

    private final Object rawToken;
    private final McpProgressReporter progressReporter;

    public CdiProgress(Object rawToken, McpProgressReporter progressReporter) {
        this.rawToken = rawToken;
        this.progressReporter = progressReporter;
    }

    @Override
    public Optional<ProgressToken> token() {
        if (rawToken == null) {
            return Optional.empty();
        }
        return Optional.of(new ProgressToken(rawToken));
    }

    @Override
    public ProgressNotification.Builder notificationBuilder() {
        return new CdiProgressNotification.CdiBuilder(rawToken, progressReporter);
    }

    @Override
    public ProgressTracker.Builder trackerBuilder() {
        return new CdiProgressTracker.CdiBuilder(rawToken, progressReporter);
    }
}
