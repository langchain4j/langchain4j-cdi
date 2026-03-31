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

@ApplicationScoped
public class McpResourceRegistry {

    private final Map<String, McpResourceDescriptor> resources = new ConcurrentHashMap<>();
    private final Map<String, McpResourceTemplateDescriptor> templates = new ConcurrentHashMap<>();

    @Inject
    McpNotificationBroadcaster broadcaster;

    @Inject
    McpResourceSubscriptionManager subscriptionManager;

    public void register(McpResourceDescriptor descriptor) {
        McpResourceDescriptor previous = resources.put(descriptor.getUri(), descriptor);
        if (previous == null) {
            notifyListChanged();
        }
    }

    public boolean unregister(String uri) {
        McpResourceDescriptor removed = resources.remove(uri);
        if (removed != null) {
            notifyListChanged();
        }
        return removed != null;
    }

    public void registerTemplate(McpResourceTemplateDescriptor descriptor) {
        McpResourceTemplateDescriptor previous = templates.put(descriptor.getUriTemplate(), descriptor);
        if (previous == null) {
            notifyListChanged();
        }
    }

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

    public Collection<McpResourceDescriptor> listResources() {
        return Collections.unmodifiableCollection(resources.values());
    }

    public Collection<McpResourceTemplateDescriptor> listTemplates() {
        return Collections.unmodifiableCollection(templates.values());
    }

    public Optional<McpResourceDescriptor> findResource(String uri) {
        return Optional.ofNullable(resources.get(uri));
    }

    public Optional<McpResourceTemplateDescriptor> findTemplate(String uriTemplate) {
        return Optional.ofNullable(templates.get(uriTemplate));
    }

    public int size() {
        return resources.size();
    }

    public int templateSize() {
        return templates.size();
    }
}
