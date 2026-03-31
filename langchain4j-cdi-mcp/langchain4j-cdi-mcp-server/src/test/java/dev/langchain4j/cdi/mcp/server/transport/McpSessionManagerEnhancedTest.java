package dev.langchain4j.cdi.mcp.server.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.langchain4j.cdi.mcp.server.error.McpSessionException;
import java.lang.reflect.Field;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class McpSessionManagerEnhancedTest {

    private static McpSessionManager createManager(Duration timeout) throws Exception {
        McpSessionManager manager = new McpSessionManager(timeout);
        setField(manager, "subscriptionManager", new McpResourceSubscriptionManager());
        setField(manager, "rootsManager", new McpRootsManager());
        return manager;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void shouldExpireSessionAfterTimeout() throws Exception {
        McpSessionManager manager = createManager(Duration.ofMillis(50));
        try {
            String sessionId = manager.createSession(null);
            assertThat(manager.activeSessionCount()).isEqualTo(1);

            Thread.sleep(100);
            manager.cleanupExpiredSessions();

            assertThat(manager.activeSessionCount()).isEqualTo(0);
            assertThatThrownBy(() -> manager.requireSession("req", sessionId)).isInstanceOf(McpSessionException.class);
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void shouldNotExpireActiveSession() throws Exception {
        McpSessionManager manager = createManager(Duration.ofMillis(200));
        try {
            String sessionId = manager.createSession(null);

            Thread.sleep(50);
            manager.requireSession("req", sessionId); // touch

            Thread.sleep(50);
            manager.cleanupExpiredSessions();

            assertThat(manager.activeSessionCount()).isEqualTo(1);
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void shouldCleanupMultipleExpiredSessions() throws Exception {
        McpSessionManager manager = createManager(Duration.ofMillis(50));
        try {
            manager.createSession(null);
            manager.createSession(null);
            manager.createSession(null);

            Thread.sleep(100);
            manager.cleanupExpiredSessions();

            assertThat(manager.activeSessionCount()).isEqualTo(0);
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void shouldUpdateLastAccessOnTouch() throws Exception {
        McpSessionManager manager = createManager(Duration.ofMinutes(30));
        try {
            String sessionId = manager.createSession(null);
            McpSession session = manager.requireSession("req", sessionId);

            assertThat(session.getLastAccessedAt()).isNotNull();
            assertThat(session.getLastAccessedAt()).isAfterOrEqualTo(session.getCreatedAt());
        } finally {
            manager.shutdown();
        }
    }
}
