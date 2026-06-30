package dev.langchain4j.cdi.mcp.server.registry;

import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcNotification;
import dev.langchain4j.cdi.mcp.server.transport.McpNotificationBroadcaster;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application-scoped registry of MCP prompt descriptors. Manages prompt registration, lookup, and broadcasts
 * list-changed notifications to connected clients when the set of prompts changes.
 */
@ApplicationScoped
public class McpPromptRegistry {

    private final Map<String, McpPromptDescriptor> prompts = new ConcurrentHashMap<>();

    @Inject
    McpNotificationBroadcaster broadcaster;

    /** CDI-required default constructor. */
    public McpPromptRegistry() {}

    /**
     * Registers a prompt descriptor. Broadcasts a list-changed notification if this is a new prompt.
     *
     * @param descriptor the prompt descriptor to register
     */
    public void register(McpPromptDescriptor descriptor) {
        McpPromptDescriptor previous = prompts.put(descriptor.getName(), descriptor);
        if (previous == null && broadcaster != null && broadcaster.connectedStreamCount() > 0) {
            broadcaster.broadcast(JsonRpcNotification.promptsListChanged());
        }
    }

    /**
     * Removes a prompt by name. Broadcasts a list-changed notification if the prompt existed.
     *
     * @param name the prompt name
     * @return {@code true} if a prompt was removed
     */
    public boolean unregister(String name) {
        McpPromptDescriptor removed = prompts.remove(name);
        if (removed != null && broadcaster != null && broadcaster.connectedStreamCount() > 0) {
            broadcaster.broadcast(JsonRpcNotification.promptsListChanged());
        }
        return removed != null;
    }

    /**
     * Returns an unmodifiable view of all registered prompt descriptors.
     *
     * @return the registered prompts
     */
    public Collection<McpPromptDescriptor> listPrompts() {
        return Collections.unmodifiableCollection(prompts.values());
    }

    /**
     * Finds a prompt descriptor by name.
     *
     * @param name the prompt name
     * @return an {@link Optional} containing the descriptor, or empty if not found
     */
    public Optional<McpPromptDescriptor> findPrompt(String name) {
        return Optional.ofNullable(prompts.get(name));
    }

    /**
     * Returns the number of registered prompts.
     *
     * @return the prompt count
     */
    public int size() {
        return prompts.size();
    }
}
