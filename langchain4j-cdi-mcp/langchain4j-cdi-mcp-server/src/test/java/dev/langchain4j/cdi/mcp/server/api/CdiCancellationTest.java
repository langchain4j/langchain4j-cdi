package dev.langchain4j.cdi.mcp.server.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mcp_java.server.Cancellation;

class CdiCancellationTest {

    @Test
    void shouldReturnNotCancelledByDefault() {
        AtomicBoolean flag = new AtomicBoolean(false);
        Cancellation cancellation = new CdiCancellation(flag);

        Cancellation.Result result = cancellation.check();
        assertThat(result.isRequested()).isFalse();
        assertThat(result.reason()).isEmpty();
    }

    @Test
    void shouldReturnCancelledWhenFlagIsSet() {
        AtomicBoolean flag = new AtomicBoolean(false);
        Cancellation cancellation = new CdiCancellation(flag);

        flag.set(true);

        Cancellation.Result result = cancellation.check();
        assertThat(result.isRequested()).isTrue();
    }
}
