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

    /** CDI-required default constructor. */
    public McpResourceSubscriptionManager() {}

    /**
     * Subscribes a session to notifications for the given resource URI.
     *
     * @param sessionId the session identifier
     * @param uri the resource URI to subscribe to
     */
    public void subscribe(String sessionId, String uri) {
        subscriptionsBySession
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(uri);
    }

    /**
     * Removes a session's subscription to the given resource URI.
     *
     * @param sessionId the session identifier
     * @param uri the resource URI to unsubscribe from
     */
    public void unsubscribe(String sessionId, String uri) {
        Set<String> uris = subscriptionsBySession.get(sessionId);
        if (uris != null) {
            uris.remove(uri);
        }
    }

    /**
     * Returns the set of session IDs subscribed to the given resource URI.
     *
     * @param uri the resource URI
     * @return set of subscribed session IDs
     */
    public Set<String> getSubscribedSessions(String uri) {
        Set<String> sessions = ConcurrentHashMap.newKeySet();
        subscriptionsBySession.forEach((sessionId, uris) -> {
            if (uris.contains(uri)) {
                sessions.add(sessionId);
            }
        });
        return sessions;
    }

    /**
     * Returns the set of resource URIs that a session is subscribed to.
     *
     * @param sessionId the session identifier
     * @return unmodifiable set of subscribed URIs, or empty set if none
     */
    public Set<String> getSubscriptions(String sessionId) {
        Set<String> uris = subscriptionsBySession.get(sessionId);
        return uris != null ? Collections.unmodifiableSet(uris) : Collections.emptySet();
    }

    /**
     * Removes all subscriptions for a session.
     *
     * @param sessionId the session identifier
     */
    public void removeSession(String sessionId) {
        subscriptionsBySession.remove(sessionId);
    }
}
