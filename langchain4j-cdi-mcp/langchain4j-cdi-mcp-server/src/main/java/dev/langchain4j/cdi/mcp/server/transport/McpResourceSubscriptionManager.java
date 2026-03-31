package dev.langchain4j.cdi.mcp.server.transport;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages resource subscriptions per session. Clients can subscribe to resource URIs to receive notifications when the
 * resource content changes.
 */
@ApplicationScoped
public class McpResourceSubscriptionManager {

    private final Map<String, Set<String>> subscriptionsBySession = new ConcurrentHashMap<>();

    public void subscribe(String sessionId, String uri) {
        subscriptionsBySession
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(uri);
    }

    public void unsubscribe(String sessionId, String uri) {
        Set<String> uris = subscriptionsBySession.get(sessionId);
        if (uris != null) {
            uris.remove(uri);
        }
    }

    public Set<String> getSubscribedSessions(String uri) {
        Set<String> sessions = ConcurrentHashMap.newKeySet();
        subscriptionsBySession.forEach((sessionId, uris) -> {
            if (uris.contains(uri)) {
                sessions.add(sessionId);
            }
        });
        return sessions;
    }

    public Set<String> getSubscriptions(String sessionId) {
        Set<String> uris = subscriptionsBySession.get(sessionId);
        return uris != null ? Collections.unmodifiableSet(uris) : Collections.emptySet();
    }

    public void removeSession(String sessionId) {
        subscriptionsBySession.remove(sessionId);
    }
}
