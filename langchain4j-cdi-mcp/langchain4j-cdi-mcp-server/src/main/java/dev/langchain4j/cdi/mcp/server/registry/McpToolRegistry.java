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

/** Thread-safe registry of discovered MCP tools, broadcasting list-changed notifications to connected clients. */
@ApplicationScoped
public class McpToolRegistry {

    /** CDI-required default constructor. */
    public McpToolRegistry() {}

    private final Map<String, McpToolDescriptor> tools = new ConcurrentHashMap<>();

    @Inject
    McpNotificationBroadcaster broadcaster;

    /**
     * Registers a tool descriptor, replacing any existing tool with the same name. Broadcasts a tools-list-changed
     * notification to connected clients when a new tool is added.
     *
     * @param descriptor the tool descriptor to register
     */
    public void register(McpToolDescriptor descriptor) {
        McpToolDescriptor previous = tools.put(descriptor.getName(), descriptor);
        if (previous == null && broadcaster != null && broadcaster.connectedStreamCount() > 0) {
            broadcaster.broadcast(JsonRpcNotification.toolsListChanged());
        }
    }

    /**
     * Removes a tool by name. Broadcasts a tools-list-changed notification if the tool existed.
     *
     * @param toolName the name of the tool to remove
     * @return {@code true} if the tool was found and removed, {@code false} otherwise
     */
    public boolean unregister(String toolName) {
        McpToolDescriptor removed = tools.remove(toolName);
        if (removed != null && broadcaster != null && broadcaster.connectedStreamCount() > 0) {
            broadcaster.broadcast(JsonRpcNotification.toolsListChanged());
        }
        return removed != null;
    }

    /**
     * Returns an unmodifiable view of all registered tool descriptors.
     *
     * @return the collection of registered tools
     */
    public Collection<McpToolDescriptor> listTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /**
     * Looks up a tool by name.
     *
     * @param name the tool name
     * @return an optional containing the descriptor, or empty if not found
     */
    public Optional<McpToolDescriptor> findTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * Returns the number of registered tools.
     *
     * @return the tool count
     */
    public int size() {
        return tools.size();
    }
}
