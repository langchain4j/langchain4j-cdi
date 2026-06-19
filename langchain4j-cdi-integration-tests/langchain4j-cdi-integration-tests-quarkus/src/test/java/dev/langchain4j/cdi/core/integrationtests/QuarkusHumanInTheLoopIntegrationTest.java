package dev.langchain4j.cdi.core.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class QuarkusHumanInTheLoopIntegrationTest {

    @ConfigProperty(name = "quarkus.http.test-port")
    int port;

    @Test
    public void testMarkerAgentIsInjectableWithCorrectName() {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target("http://localhost:" + port + "/hitl-agent/marker");
            Response response = target.request(MediaType.TEXT_PLAIN).get();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(String.class)).isEqualTo("Agent[hitl-marker-agent]");
        }
    }

    @Test
    public void testEntryMethodAgentIsInjectableWithCorrectName() {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target("http://localhost:" + port + "/hitl-agent/entry-method");
            Response response = target.request(MediaType.TEXT_PLAIN).get();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(String.class)).isEqualTo("Agent[hitl-entry-method-agent]");
        }
    }
}
