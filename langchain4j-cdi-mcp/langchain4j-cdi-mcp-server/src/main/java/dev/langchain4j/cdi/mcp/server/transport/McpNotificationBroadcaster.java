package dev.langchain4j.cdi.mcp.server.transport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Broadcasts MCP notifications to connected SSE streams. Maintains a registry of active output streams keyed by session
 * ID and delivers JSON-RPC notifications to one or all connected clients.
 */
@ApplicationScoped
public class McpNotificationBroadcaster {

    private static final Logger LOGGER = Logger.getLogger(McpNotificationBroadcaster.class.getName());

    private final Map<String, OutputStream> sseStreams = new ConcurrentHashMap<>();

    /** CDI-required default constructor. */
    public McpNotificationBroadcaster() {}

    /**
     * Registers an SSE output stream for a session.
     *
     * @param sessionId the session identifier
     * @param out the output stream to send notifications to
     */
    public void registerStream(String sessionId, OutputStream out) {
        sseStreams.put(sessionId, out);
    }

    /**
     * Removes the SSE output stream for a session.
     *
     * @param sessionId the session identifier
     */
    public void unregisterStream(String sessionId) {
        sseStreams.remove(sessionId);
    }

    /**
     * Sends a notification to all connected SSE streams. Disconnected streams are automatically removed.
     *
     * @param notification the notification object to serialize and broadcast
     */
    public void broadcast(Object notification) {
        String json = serializeToJson(notification);
        String payload = "event: message\ndata: " + json + "\n\n";
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

        sseStreams.entrySet().removeIf(entry -> {
            try {
                entry.getValue().write(bytes);
                entry.getValue().flush();
                return false;
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "MCP: Removing disconnected SSE stream: " + entry.getKey(), e);
                return true;
            }
        });
    }

    /**
     * Sends a notification to a specific session's SSE stream.
     *
     * @param sessionId the target session identifier
     * @param notification the notification object to serialize and send
     */
    public void sendToSession(String sessionId, Object notification) {
        OutputStream out = sseStreams.get(sessionId);
        if (out == null) {
            return;
        }
        String json = serializeToJson(notification);
        String payload = "event: message\ndata: " + json + "\n\n";
        try {
            out.write(payload.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "MCP: Removing disconnected SSE stream: " + sessionId, e);
            sseStreams.remove(sessionId);
        }
    }

    /**
     * Returns the number of currently connected SSE streams.
     *
     * @return the count of active SSE streams
     */
    public int connectedStreamCount() {
        return sseStreams.size();
    }

    private String serializeToJson(Object obj) {
        JsonbConfig config = new JsonbConfig().withNullValues(false);
        try (Jsonb jsonb = JsonbBuilder.create(config)) {
            return jsonb.toJson(obj);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "MCP: Failed to serialize notification", e);
            return "{}";
        }
    }
}
