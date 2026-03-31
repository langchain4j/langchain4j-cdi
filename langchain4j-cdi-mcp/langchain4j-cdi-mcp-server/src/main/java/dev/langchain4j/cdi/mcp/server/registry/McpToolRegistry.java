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
public class McpToolRegistry {

    private final Map<String, McpToolDescriptor> tools = new ConcurrentHashMap<>();

    @Inject
    McpNotificationBroadcaster broadcaster;

    public void register(McpToolDescriptor descriptor) {
        McpToolDescriptor previous = tools.put(descriptor.getName(), descriptor);
        if (previous == null && broadcaster != null && broadcaster.connectedStreamCount() > 0) {
            broadcaster.broadcast(JsonRpcNotification.toolsListChanged());
        }
    }

    public boolean unregister(String toolName) {
        McpToolDescriptor removed = tools.remove(toolName);
        if (removed != null && broadcaster != null && broadcaster.connectedStreamCount() > 0) {
            broadcaster.broadcast(JsonRpcNotification.toolsListChanged());
        }
        return removed != null;
    }

    public Collection<McpToolDescriptor> listTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    public Optional<McpToolDescriptor> findTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public int size() {
        return tools.size();
    }
}
