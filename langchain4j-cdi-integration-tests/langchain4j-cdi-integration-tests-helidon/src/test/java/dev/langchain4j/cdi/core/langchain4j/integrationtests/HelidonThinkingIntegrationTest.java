package dev.langchain4j.cdi.core.langchain4j.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.cdi.integrationtests.ThinkingChatModelMock;
import dev.langchain4j.cdi.integrationtests.ThinkingTestHelper;
import io.helidon.microprofile.testing.junit5.HelidonTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import org.junit.jupiter.api.Test;

@HelidonTest
public class HelidonThinkingIntegrationTest {

    @Inject
    WebTarget injectedTarget;

    @Test
    public void testOnThinkingHandlerCapturesThinking() {
        String result = ThinkingTestHelper.postAndGetThinking(injectedTarget, "on-thinking");
        assertThat(result).isEqualTo(ThinkingChatModelMock.THINKING_TEXT);
    }

    @Test
    public void testListenerNameCapturesThinking() {
        String result = ThinkingTestHelper.postAndGetThinking(injectedTarget, "listener");
        assertThat(result).isEqualTo(ThinkingChatModelMock.THINKING_TEXT);
    }
}
