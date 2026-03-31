package dev.langchain4j.cdi.mcp.server.transport;

import dev.langchain4j.cdi.mcp.server.error.McpSessionException;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ApplicationScoped
public class McpSessionManager {

    private static final Logger LOGGER = Logger.getLogger(McpSessionManager.class.getName());
    private static final Duration DEFAULT_SESSION_TIMEOUT = Duration.ofMinutes(30);
    private static final long CLEANUP_INTERVAL_SECONDS = 60;

    private final ConcurrentHashMap<String, McpSession> sessions = new ConcurrentHashMap<>();
    private final Duration sessionTimeout;
    private final ScheduledExecutorService cleanupExecutor;

    @Inject
    McpResourceSubscriptionManager subscriptionManager;

    @Inject
    McpRootsManager rootsManager;

    public McpSessionManager() {
        this(DEFAULT_SESSION_TIMEOUT);
    }

    public McpSessionManager(Duration sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mcp-session-cleanup");
            t.setDaemon(true);
            return t;
        });
        this.cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredSessions, CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    void shutdown() {
        cleanupExecutor.shutdownNow();
    }

    public String createSession(JsonObject initParams) {
        String id = UUID.randomUUID().toString();
        sessions.put(id, new McpSession(id, initParams));
        return id;
    }

    public McpSession requireSession(Object requestId, String sessionId) {
        if (sessionId == null || !sessions.containsKey(sessionId)) {
            throw new McpSessionException(requestId, "Invalid or missing Mcp-Session-Id");
        }
        McpSession session = sessions.get(sessionId);
        session.touch();
        return session;
    }

    public void terminateSession(String sessionId) {
        McpSession removed = sessions.remove(sessionId);
        if (removed != null) {
            subscriptionManager.removeSession(sessionId);
            rootsManager.removeSession(sessionId);
            LOGGER.fine("MCP: Session terminated: " + sessionId);
        }
    }

    public int activeSessionCount() {
        return sessions.size();
    }

    void cleanupExpiredSessions() {
        Instant cutoff = Instant.now().minus(sessionTimeout);
        sessions.entrySet().removeIf(entry -> {
            if (entry.getValue().getLastAccessedAt().isBefore(cutoff)) {
                String sessionId = entry.getKey();
                subscriptionManager.removeSession(sessionId);
                rootsManager.removeSession(sessionId);
                LOGGER.info("MCP: Session expired: " + sessionId);
                return true;
            }
            return false;
        });
    }
}
