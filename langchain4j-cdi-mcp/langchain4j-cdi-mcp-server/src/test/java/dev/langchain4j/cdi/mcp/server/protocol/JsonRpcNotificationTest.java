package dev.langchain4j.cdi.mcp.server.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JsonRpcNotificationTest {

    @Test
    void shouldCreateToolsListChangedNotification() {
        JsonRpcNotification notification = JsonRpcNotification.toolsListChanged();

        assertThat(notification.getJsonrpc()).isEqualTo("2.0");
        assertThat(notification.getMethod()).isEqualTo("notifications/tools/list_changed");
    }

    @Test
    void shouldCreateResourcesListChangedNotification() {
        JsonRpcNotification notification = JsonRpcNotification.resourcesListChanged();

        assertThat(notification.getJsonrpc()).isEqualTo("2.0");
        assertThat(notification.getMethod()).isEqualTo("notifications/resources/list_changed");
        assertThat(notification.getParams()).isNull();
    }

    @Test
    void shouldCreateResourceUpdatedNotification() {
        JsonRpcNotification notification = JsonRpcNotification.resourceUpdated("config://app");

        assertThat(notification.getMethod()).isEqualTo("notifications/resources/updated");
        assertThat(notification.getParams()).isNotNull();
    }

    @Test
    void shouldCreatePromptsListChangedNotification() {
        JsonRpcNotification notification = JsonRpcNotification.promptsListChanged();

        assertThat(notification.getMethod()).isEqualTo("notifications/prompts/list_changed");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateProgressNotification() {
        JsonRpcNotification notification = JsonRpcNotification.progress("token-42", 50.0, 100.0);

        assertThat(notification.getMethod()).isEqualTo("notifications/progress");
        assertThat(notification.getParams()).isNotNull();
        java.util.Map<String, Object> params = (java.util.Map<String, Object>) notification.getParams();
        assertThat(params.get("progressToken")).isEqualTo("token-42");
        assertThat(params.get("progress")).isEqualTo(50.0);
        assertThat(params.get("total")).isEqualTo(100.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldOmitTotalWhenZero() {
        JsonRpcNotification notification = JsonRpcNotification.progress("t", 5.0, 0);

        java.util.Map<String, Object> params = (java.util.Map<String, Object>) notification.getParams();
        assertThat(params).doesNotContainKey("total");
    }
}
