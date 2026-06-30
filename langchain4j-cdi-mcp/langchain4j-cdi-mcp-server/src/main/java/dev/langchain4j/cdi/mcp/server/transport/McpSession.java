package dev.langchain4j.cdi.mcp.server.transport;

import jakarta.json.JsonObject;
import java.time.Instant;

/**
 * Represents an active MCP client session, tracking its identity, capabilities, initialization state, and last access
 * time.
 */
public class McpSession {

    private final String id;
    private final Instant createdAt;
    private final JsonObject clientCapabilities;
    private volatile boolean initialized;
    private volatile Instant lastAccessedAt;

    /**
     * Creates a new session with the given id and client capabilities.
     *
     * @param id the unique session identifier
     * @param clientCapabilities the capabilities declared by the client during initialization
     */
    public McpSession(String id, JsonObject clientCapabilities) {
        this.id = id;
        this.createdAt = Instant.now();
        this.lastAccessedAt = this.createdAt;
        this.clientCapabilities = clientCapabilities;
        this.initialized = false;
    }

    /**
     * Returns the unique session identifier.
     *
     * @return the session id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the instant when this session was created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the instant when this session was last accessed.
     *
     * @return the last access timestamp
     */
    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    /**
     * Returns the client capabilities declared during initialization.
     *
     * @return the client capabilities as a JsonObject, or null if none were provided
     */
    public JsonObject getClientCapabilities() {
        return clientCapabilities;
    }

    /**
     * Returns whether this session has completed the MCP initialization handshake.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /** Marks this session as initialized and updates the last access time. */
    public void markInitialized() {
        this.initialized = true;
        touch();
    }

    /** Updates the last access timestamp to the current time. */
    public void touch() {
        this.lastAccessedAt = Instant.now();
    }

    /**
     * Checks if the client declared a given capability during initialization.
     *
     * @param name the capability name
     * @return {@code true} if the client declared the capability
     */
    public boolean hasCapability(String name) {
        if (clientCapabilities == null) {
            return false;
        }
        return clientCapabilities.containsKey(name);
    }
}
