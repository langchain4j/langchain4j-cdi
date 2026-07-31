package dev.langchain4j.cdi.core.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.cdi.integrationtests.ThinkingChatModelMock;
import dev.langchain4j.cdi.integrationtests.ThinkingTestHelper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class QuarkusThinkingIntegrationTest {

    @ConfigProperty(name = "quarkus.http.test-port")
    int port;

    @Test
    public void testOnThinkingHandlerCapturesThinking() {
        try (Client client = ClientBuilder.newClient()) {
            String result =
                    ThinkingTestHelper.postAndGetThinking(client.target("http://localhost:" + port), "on-thinking");
            assertThat(result).isEqualTo(ThinkingChatModelMock.THINKING_TEXT);
        }
    }

    @Test
    public void testListenerNameCapturesThinking() {
        try (Client client = ClientBuilder.newClient()) {
            String result =
                    ThinkingTestHelper.postAndGetThinking(client.target("http://localhost:" + port), "listener");
            assertThat(result).isEqualTo(ThinkingChatModelMock.THINKING_TEXT);
        }
    }
}
