package dev.langchain4j.cdi.mcp.server.transport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpProgressReporterTest {

    McpProgressReporter reporter;
    McpNotificationBroadcaster broadcaster;

    @BeforeEach
    void setup() throws Exception {
        reporter = new McpProgressReporter();
        broadcaster = mock(McpNotificationBroadcaster.class);
        Field field = McpProgressReporter.class.getDeclaredField("broadcaster");
        field.setAccessible(true);
        field.set(reporter, broadcaster);
    }

    @Test
    void shouldBroadcastProgressWhenTokenPresent() {
        when(broadcaster.connectedStreamCount()).thenReturn(1);

        reporter.reportProgress("token-123", 50.0, 100.0);

        verify(broadcaster).broadcast(any());
    }

    @Test
    void shouldNotBroadcastWhenNoToken() {
        reporter.reportProgress(null, 50.0, 100.0);

        verify(broadcaster, never()).broadcast(any());
    }

    @Test
    void shouldNotBroadcastWhenNoConnectedStreams() {
        when(broadcaster.connectedStreamCount()).thenReturn(0);

        reporter.reportProgress("token-123", 50.0, 100.0);

        verify(broadcaster, never()).broadcast(any());
    }
}
