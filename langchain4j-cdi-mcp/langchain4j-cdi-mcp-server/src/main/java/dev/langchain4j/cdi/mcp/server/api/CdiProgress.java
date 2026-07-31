package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.transport.McpProgressReporter;
import java.util.Optional;
import org.mcpjava.server.progress.Progress;
import org.mcpjava.server.progress.ProgressNotification;
import org.mcpjava.server.progress.ProgressToken;
import org.mcpjava.server.progress.ProgressTracker;

/** Implementation of {@link Progress} that wraps a progress token and delegates to {@link McpProgressReporter}. */
public class CdiProgress implements Progress {

    private final Object rawToken;
    private final McpProgressReporter progressReporter;

    /**
     * Creates a new progress wrapper.
     *
     * @param rawToken the raw progress token, or {@code null} if no token was provided
     * @param progressReporter the progress reporter
     */
    public CdiProgress(Object rawToken, McpProgressReporter progressReporter) {
        this.rawToken = rawToken;
        this.progressReporter = progressReporter;
    }

    @Override
    public Optional<ProgressToken> token() {
        if (rawToken == null) {
            return Optional.empty();
        }
        return Optional.of(CdiProgressToken.of(rawToken));
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
