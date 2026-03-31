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

@ApplicationScoped
public class McpNotificationBroadcaster {

    private static final Logger LOGGER = Logger.getLogger(McpNotificationBroadcaster.class.getName());

    private final Map<String, OutputStream> sseStreams = new ConcurrentHashMap<>();

    public void registerStream(String sessionId, OutputStream out) {
        sseStreams.put(sessionId, out);
    }

    public void unregisterStream(String sessionId) {
        sseStreams.remove(sessionId);
    }

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
