package dev.langchain4j.cdi.mcp.server.transport;

import jakarta.json.JsonObject;
import java.time.Instant;

public class McpSession {

    private final String id;
    private final Instant createdAt;
    private final JsonObject clientCapabilities;
    private volatile boolean initialized;
    private volatile Instant lastAccessedAt;

    public McpSession(String id, JsonObject clientCapabilities) {
        this.id = id;
        this.createdAt = Instant.now();
        this.lastAccessedAt = this.createdAt;
        this.clientCapabilities = clientCapabilities;
        this.initialized = false;
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public JsonObject getClientCapabilities() {
        return clientCapabilities;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void markInitialized() {
        this.initialized = true;
        touch();
    }

    public void touch() {
        this.lastAccessedAt = Instant.now();
    }

    /** Checks if the client declared a given capability during initialization. */
    public boolean hasCapability(String name) {
        if (clientCapabilities == null) {
            return false;
        }
        return clientCapabilities.containsKey(name);
    }
}
