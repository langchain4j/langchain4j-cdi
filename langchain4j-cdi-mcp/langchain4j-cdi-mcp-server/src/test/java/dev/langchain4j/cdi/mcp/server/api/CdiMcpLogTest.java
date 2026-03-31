package dev.langchain4j.cdi.mcp.server.api;

import static org.mockito.Mockito.*;

import dev.langchain4j.cdi.mcp.server.logging.McpLogLevel;
import dev.langchain4j.cdi.mcp.server.logging.McpLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mcp_java.server.McpLog;

class CdiMcpLogTest {

    McpLogger mcpLogger;
    CdiMcpLog log;

    @BeforeEach
    void setup() {
        mcpLogger = mock(McpLogger.class);
        when(mcpLogger.getMinimumLevel()).thenReturn(McpLogLevel.debug);
        log = new CdiMcpLog(mcpLogger, "TestTool");
    }

    @Test
    void shouldDelegateInfoToMcpLogger() {
        log.info("Hello {}", "world");
        verify(mcpLogger).log(McpLogLevel.info, "TestTool", "Hello world");
    }

    @Test
    void shouldDelegateDebugToMcpLogger() {
        log.debug("Debug {}", "msg");
        verify(mcpLogger).log(McpLogLevel.debug, "TestTool", "Debug msg");
    }

    @Test
    void shouldDelegateErrorToMcpLogger() {
        log.error("Error {}", "details");
        verify(mcpLogger).log(McpLogLevel.error, "TestTool", "Error details");
    }

    @Test
    void shouldDelegateErrorWithThrowable() {
        Exception ex = new RuntimeException("boom");
        log.error(ex, "Failed {}", "here");
        verify(mcpLogger).log(McpLogLevel.error, "TestTool", "Failed here - boom");
    }

    @Test
    void shouldDelegateSendWithLevel() {
        log.send(McpLog.LogLevel.WARNING, "warn message");
        verify(mcpLogger).log(McpLogLevel.warning, "TestTool", "warn message");
    }

    @Test
    void shouldReturnCurrentLevel() {
        when(mcpLogger.getMinimumLevel()).thenReturn(McpLogLevel.error);
        org.assertj.core.api.Assertions.assertThat(log.level()).isEqualTo(McpLog.LogLevel.ERROR);
    }
}
