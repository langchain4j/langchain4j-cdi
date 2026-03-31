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

@ApplicationScoped
public class McpPromptRegistry {

    private final Map<String, McpPromptDescriptor> prompts = new ConcurrentHashMap<>();

    @Inject
    McpNotificationBroadcaster broadcaster;

    public void register(McpPromptDescriptor descriptor) {
        McpPromptDescriptor previous = prompts.put(descriptor.getName(), descriptor);
        if (previous == null && broadcaster != null && broadcaster.connectedStreamCount() > 0) {
            broadcaster.broadcast(JsonRpcNotification.promptsListChanged());
        }
    }

    public boolean unregister(String name) {
        McpPromptDescriptor removed = prompts.remove(name);
        if (removed != null && broadcaster != null && broadcaster.connectedStreamCount() > 0) {
            broadcaster.broadcast(JsonRpcNotification.promptsListChanged());
        }
        return removed != null;
    }

    public Collection<McpPromptDescriptor> listPrompts() {
        return Collections.unmodifiableCollection(prompts.values());
    }

    public Optional<McpPromptDescriptor> findPrompt(String name) {
        return Optional.ofNullable(prompts.get(name));
    }

    public int size() {
        return prompts.size();
    }
}
