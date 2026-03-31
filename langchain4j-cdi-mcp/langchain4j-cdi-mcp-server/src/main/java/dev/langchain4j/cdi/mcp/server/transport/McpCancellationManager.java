package dev.langchain4j.cdi.mcp.server.transport;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks cancellation state per JSON-RPC request. When a {@code notifications/cancelled} is received, the corresponding
 * {@link AtomicBoolean} flag is set to {@code true}.
 */
@ApplicationScoped
public class McpCancellationManager {

    private final Map<Object, AtomicBoolean> flags = new ConcurrentHashMap<>();

    /** Registers a new request and returns the cancellation flag. */
    public AtomicBoolean register(Object requestId) {
        AtomicBoolean flag = new AtomicBoolean(false);
        flags.put(requestId, flag);
        return flag;
    }

    /** Marks a request as cancelled. */
    public void cancel(Object requestId) {
        AtomicBoolean flag = flags.get(requestId);
        if (flag != null) {
            flag.set(true);
        }
    }

    /** Removes the flag for a completed request. */
    public void unregister(Object requestId) {
        flags.remove(requestId);
    }
}
