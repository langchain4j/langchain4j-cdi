package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.transport.McpProgressReporter;
import java.util.Optional;
import org.mcp_java.server.progress.Progress;
import org.mcp_java.server.progress.ProgressNotification;
import org.mcp_java.server.progress.ProgressToken;
import org.mcp_java.server.progress.ProgressTracker;

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
        return Optional.of(progressToken(rawToken));
    }

    @Override
    public ProgressNotification.Builder notificationBuilder() {
        return new CdiProgressNotification.CdiBuilder(rawToken, progressReporter);
    }

    @Override
    public ProgressTracker.Builder trackerBuilder() {
        return new CdiProgressTracker.CdiBuilder(rawToken, progressReporter);
    }

    static ProgressToken progressToken(Object rawToken) {
        if (rawToken instanceof Number n) {
            return new ProgressToken() {
                @Override
                public Type type() {
                    return Type.INTEGER;
                }

                @Override
                public Number asInteger() {
                    return n;
                }

                @Override
                public String asString() {
                    return null;
                }
            };
        }
        String s = rawToken.toString();
        return new ProgressToken() {
            @Override
            public Type type() {
                return Type.STRING;
            }

            @Override
            public Number asInteger() {
                return null;
            }

            @Override
            public String asString() {
                return s;
            }
        };
    }
}
