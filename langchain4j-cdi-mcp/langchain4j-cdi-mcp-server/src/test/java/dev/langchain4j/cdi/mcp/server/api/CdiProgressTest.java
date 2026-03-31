package dev.langchain4j.cdi.mcp.server.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import dev.langchain4j.cdi.mcp.server.transport.McpProgressReporter;
import org.junit.jupiter.api.Test;
import org.mcp_java.server.Progress;
import org.mcp_java.server.ProgressNotification;
import org.mcp_java.server.ProgressTracker;

class CdiProgressTest {

    @Test
    void shouldReturnEmptyTokenWhenNull() {
        McpProgressReporter reporter = mock(McpProgressReporter.class);
        Progress progress = new CdiProgress(null, reporter);

        assertThat(progress.token()).isEmpty();
    }

    @Test
    void shouldReturnTokenWhenPresent() {
        McpProgressReporter reporter = mock(McpProgressReporter.class);
        Progress progress = new CdiProgress("tok-123", reporter);

        assertThat(progress.token()).isPresent();
        assertThat(progress.token().get().value()).isEqualTo("tok-123");
    }

    @Test
    void shouldBuildAndSendNotification() {
        McpProgressReporter reporter = mock(McpProgressReporter.class);
        Progress progress = new CdiProgress("tok-1", reporter);

        ProgressNotification notification = progress.notificationBuilder()
                .setProgress(5)
                .setTotal(10)
                .setMessage("halfway")
                .build();

        notification.sendAndForget();

        verify(reporter).reportProgress("tok-1", 5.0, 10.0, "halfway");
    }

    @Test
    void shouldBuildAndAdvanceTracker() {
        McpProgressReporter reporter = mock(McpProgressReporter.class);
        Progress progress = new CdiProgress("tok-2", reporter);

        ProgressTracker tracker =
                progress.trackerBuilder().setTotal(100).setDefaultStep(10).build();

        assertThat(tracker.progress().intValue()).isEqualTo(0);
        assertThat(tracker.total().intValue()).isEqualTo(100);

        tracker.advanceAndForget();
        assertThat(tracker.progress().intValue()).isEqualTo(10);
        verify(reporter).reportProgress(eq("tok-2"), eq(10.0), eq(100.0), isNull());

        tracker.advanceAndForget();
        assertThat(tracker.progress().intValue()).isEqualTo(20);
    }
}
