package dev.langchain4j.cdi.mcp.server.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.langchain4j.cdi.mcp.server.error.McpSessionException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpSessionManagerTest {

    McpSessionManager manager;

    @BeforeEach
    void setUp() throws Exception {
        manager = new McpSessionManager();
        setField(manager, "subscriptionManager", new McpResourceSubscriptionManager());
        setField(manager, "rootsManager", new McpRootsManager());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void shouldCreateSession() {
        String sessionId = manager.createSession(null);
        assertThat(sessionId).isNotNull().isNotBlank();
        assertThat(manager.activeSessionCount()).isEqualTo(1);
    }

    @Test
    void shouldRequireValidSession() {
        String sessionId = manager.createSession(null);
        McpSession session = manager.requireSession("req-1", sessionId);
        assertThat(session).isNotNull();
        assertThat(session.getId()).isEqualTo(sessionId);
    }

    @Test
    void shouldThrowForNullSessionId() {
        assertThatThrownBy(() -> manager.requireSession("req-1", null))
                .isInstanceOf(McpSessionException.class)
                .hasMessageContaining("Invalid or missing");
    }

    @Test
    void shouldThrowForUnknownSessionId() {
        assertThatThrownBy(() -> manager.requireSession("req-1", "unknown-id")).isInstanceOf(McpSessionException.class);
    }

    @Test
    void shouldTerminateSession() {
        String sessionId = manager.createSession(null);
        assertThat(manager.activeSessionCount()).isEqualTo(1);

        manager.terminateSession(sessionId);
        assertThat(manager.activeSessionCount()).isEqualTo(0);
    }

    @Test
    void shouldCreateMultipleSessions() {
        manager.createSession(null);
        manager.createSession(null);
        manager.createSession(null);
        assertThat(manager.activeSessionCount()).isEqualTo(3);
    }
}
