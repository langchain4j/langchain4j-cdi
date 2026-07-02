package dev.langchain4j.cdi.mcp.server.registry;

import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcNotification;
import dev.langchain4j.cdi.mcp.server.transport.McpNotificationBroadcaster;
import dev.langchain4j.cdi.mcp.server.transport.McpResourceSubscriptionManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application-scoped registry of MCP resource and resource-template descriptors. Manages registration, lookup, and
 * broadcasts list-changed and resource-updated notifications to connected clients.
 */
@ApplicationScoped
public class McpResourceRegistry {

    private final Map<String, McpResourceDescriptor> resources = new ConcurrentHashMap<>();
    private final Map<String, McpResourceTemplateDescriptor> templates = new ConcurrentHashMap<>();

    @Inject
    McpNotificationBroadcaster broadcaster;

    @Inject
    McpResourceSubscriptionManager subscriptionManager;

    /** CDI-required default constructor. */
    public McpResourceRegistry() {}

    /**
     * Registers a resource descriptor. Broadcasts a list-changed notification if this is a new resource.
     *
     * @param descriptor the resource descriptor to register
     */
    public void register(McpResourceDescriptor descriptor) {
        McpResourceDescriptor previous = resources.put(descriptor.getUri(), descriptor);
        if (previous == null) {
            notifyListChanged();
        }
    }

    /**
     * Removes a resource by URI. Broadcasts a list-changed notification if the resource existed.
     *
     * @param uri the resource URI
     * @return {@code true} if a resource was removed
     */
    public boolean unregister(String uri) {
        McpResourceDescriptor removed = resources.remove(uri);
        if (removed != null) {
            notifyListChanged();
        }
        return removed != null;
    }

    /**
     * Registers a resource template descriptor. Broadcasts a list-changed notification if this is a new template.
     *
     * @param descriptor the resource template descriptor to register
     */
    public void registerTemplate(McpResourceTemplateDescriptor descriptor) {
        McpResourceTemplateDescriptor previous = templates.put(descriptor.getUriTemplate(), descriptor);
        if (previous == null) {
            notifyListChanged();
        }
    }

    /**
     * Sends a resource-updated notification to all sessions subscribed to the given URI.
     *
     * @param uri the URI of the updated resource
     */
    public void notifyResourceUpdated(String uri) {
        if (broadcaster == null || subscriptionManager == null) {
            return;
        }
        JsonRpcNotification notification = JsonRpcNotification.resourceUpdated(uri);
        for (String sessionId : subscriptionManager.getSubscribedSessions(uri)) {
            broadcaster.sendToSession(sessionId, notification);
        }
    }

    private void notifyListChanged() {
        if (broadcaster != null && broadcaster.connectedStreamCount() > 0) {
            broadcaster.broadcast(JsonRpcNotification.resourcesListChanged());
        }
    }

    /**
     * Returns an unmodifiable view of all registered resource descriptors.
     *
     * @return the registered resources
     */
    public Collection<McpResourceDescriptor> listResources() {
        return Collections.unmodifiableCollection(resources.values());
    }

    /**
     * Returns an unmodifiable view of all registered resource template descriptors.
     *
     * @return the registered resource templates
     */
    public Collection<McpResourceTemplateDescriptor> listTemplates() {
        return Collections.unmodifiableCollection(templates.values());
    }

    /**
     * Finds a resource descriptor by URI.
     *
     * @param uri the resource URI
     * @return an {@link Optional} containing the descriptor, or empty if not found
     */
    public Optional<McpResourceDescriptor> findResource(String uri) {
        return Optional.ofNullable(resources.get(uri));
    }

    /**
     * Finds a resource template descriptor by URI template.
     *
     * @param uriTemplate the URI template string
     * @return an {@link Optional} containing the descriptor, or empty if not found
     */
    public Optional<McpResourceTemplateDescriptor> findTemplate(String uriTemplate) {
        return Optional.ofNullable(templates.get(uriTemplate));
    }

    /**
     * Returns the number of registered resources.
     *
     * @return the resource count
     */
    public int size() {
        return resources.size();
    }

    /**
     * Returns the number of registered resource templates.
     *
     * @return the template count
     */
    public int templateSize() {
        return templates.size();
    }
}
