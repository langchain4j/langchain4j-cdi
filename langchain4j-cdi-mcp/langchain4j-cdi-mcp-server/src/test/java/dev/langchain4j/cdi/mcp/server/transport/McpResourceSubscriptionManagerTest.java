package dev.langchain4j.cdi.mcp.server.transport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class McpResourceSubscriptionManagerTest {

    @Test
    void shouldSubscribeAndFindSubscribedSessions() {
        McpResourceSubscriptionManager manager = new McpResourceSubscriptionManager();
        manager.subscribe("session-1", "config://app");
        manager.subscribe("session-2", "config://app");
        manager.subscribe("session-1", "data://status");

        assertThat(manager.getSubscribedSessions("config://app")).containsExactlyInAnyOrder("session-1", "session-2");
        assertThat(manager.getSubscribedSessions("data://status")).containsExactly("session-1");
        assertThat(manager.getSubscriptions("session-1")).containsExactlyInAnyOrder("config://app", "data://status");
    }

    @Test
    void shouldUnsubscribe() {
        McpResourceSubscriptionManager manager = new McpResourceSubscriptionManager();
        manager.subscribe("session-1", "config://app");
        manager.unsubscribe("session-1", "config://app");

        assertThat(manager.getSubscribedSessions("config://app")).isEmpty();
    }

    @Test
    void shouldRemoveSession() {
        McpResourceSubscriptionManager manager = new McpResourceSubscriptionManager();
        manager.subscribe("session-1", "config://app");
        manager.subscribe("session-1", "data://status");
        manager.removeSession("session-1");

        assertThat(manager.getSubscribedSessions("config://app")).isEmpty();
        assertThat(manager.getSubscriptions("session-1")).isEmpty();
    }
}
