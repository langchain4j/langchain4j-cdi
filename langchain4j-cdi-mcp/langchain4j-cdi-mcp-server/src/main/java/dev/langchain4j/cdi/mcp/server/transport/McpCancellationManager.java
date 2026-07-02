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

    /** Creates a new cancellation manager with an empty flag map. */
    public McpCancellationManager() {}

    /**
     * Registers a new request and returns its cancellation flag.
     *
     * @param requestId the JSON-RPC request identifier
     * @return an {@link AtomicBoolean} that will be set to {@code true} if the request is cancelled
     */
    public AtomicBoolean register(Object requestId) {
        AtomicBoolean flag = new AtomicBoolean(false);
        flags.put(requestId, flag);
        return flag;
    }

    /**
     * Marks a request as cancelled by setting its flag to {@code true}.
     *
     * @param requestId the JSON-RPC request identifier to cancel
     */
    public void cancel(Object requestId) {
        AtomicBoolean flag = flags.get(requestId);
        if (flag != null) {
            flag.set(true);
        }
    }

    /**
     * Removes the cancellation flag for a completed request.
     *
     * @param requestId the JSON-RPC request identifier to unregister
     */
    public void unregister(Object requestId) {
        flags.remove(requestId);
    }
}
