package dev.langchain4j.cdi.mcp.server.transport;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcNotification;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class McpNotificationBroadcasterTest {

    @Test
    void shouldBroadcastToRegisteredStreams() {
        McpNotificationBroadcaster broadcaster = new McpNotificationBroadcaster();
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();

        broadcaster.registerStream("s1", out1);
        broadcaster.registerStream("s2", out2);

        broadcaster.broadcast(JsonRpcNotification.toolsListChanged());

        String output1 = out1.toString();
        String output2 = out2.toString();
        assertThat(output1).contains("event: message");
        assertThat(output1).contains("notifications/tools/list_changed");
        assertThat(output2).contains("event: message");
    }

    @Test
    void shouldUnregisterStream() {
        McpNotificationBroadcaster broadcaster = new McpNotificationBroadcaster();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        broadcaster.registerStream("s1", out);
        assertThat(broadcaster.connectedStreamCount()).isEqualTo(1);

        broadcaster.unregisterStream("s1");
        assertThat(broadcaster.connectedStreamCount()).isEqualTo(0);
    }

    @Test
    void shouldRemoveDisconnectedStreamsOnBroadcast() {
        McpNotificationBroadcaster broadcaster = new McpNotificationBroadcaster();

        // Create a stream that throws on write (simulates disconnection)
        java.io.OutputStream failingStream = new java.io.OutputStream() {
            @Override
            public void write(int b) throws java.io.IOException {
                throw new java.io.IOException("disconnected");
            }
        };

        broadcaster.registerStream("s1", failingStream);
        assertThat(broadcaster.connectedStreamCount()).isEqualTo(1);

        broadcaster.broadcast(JsonRpcNotification.toolsListChanged());

        assertThat(broadcaster.connectedStreamCount()).isEqualTo(0);
    }

    @Test
    void shouldHandleEmptyBroadcast() {
        McpNotificationBroadcaster broadcaster = new McpNotificationBroadcaster();
        // Should not throw
        broadcaster.broadcast(JsonRpcNotification.toolsListChanged());
        assertThat(broadcaster.connectedStreamCount()).isEqualTo(0);
    }
}
