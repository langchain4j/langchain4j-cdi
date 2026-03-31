package dev.langchain4j.cdi.mcp.server.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import dev.langchain4j.cdi.mcp.server.transport.McpNotificationBroadcaster;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpLoggerTest {

    McpLogger logger;
    McpNotificationBroadcaster broadcaster;

    @BeforeEach
    void setup() throws Exception {
        logger = new McpLogger();
        broadcaster = mock(McpNotificationBroadcaster.class);
        Field field = McpLogger.class.getDeclaredField("broadcaster");
        field.setAccessible(true);
        field.set(logger, broadcaster);
    }

    @Test
    void shouldSetMinimumLevel() {
        logger.setMinimumLevel(McpLogLevel.warning);
        assertThat(logger.getMinimumLevel()).isEqualTo(McpLogLevel.warning);
    }

    @Test
    void shouldBroadcastWhenAboveMinimumLevel() {
        when(broadcaster.connectedStreamCount()).thenReturn(1);
        logger.setMinimumLevel(McpLogLevel.info);

        logger.error("test", "an error");

        verify(broadcaster).broadcast(any());
    }

    @Test
    void shouldNotBroadcastBelowMinimumLevel() {
        when(broadcaster.connectedStreamCount()).thenReturn(1);
        logger.setMinimumLevel(McpLogLevel.warning);

        logger.debug("test", "debug message");

        verify(broadcaster, never()).broadcast(any());
    }

    @Test
    void shouldNotBroadcastWithNoStreams() {
        when(broadcaster.connectedStreamCount()).thenReturn(0);
        logger.setMinimumLevel(McpLogLevel.debug);

        logger.info("test", "a message");

        verify(broadcaster, never()).broadcast(any());
    }
}
