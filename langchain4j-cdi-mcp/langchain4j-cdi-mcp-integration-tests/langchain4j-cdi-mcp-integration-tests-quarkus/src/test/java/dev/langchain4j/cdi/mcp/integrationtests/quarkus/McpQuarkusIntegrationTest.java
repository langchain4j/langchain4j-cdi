package dev.langchain4j.cdi.mcp.integrationtests.quarkus;

import dev.langchain4j.cdi.mcp.integrationtests.AbstractMcpIntegrationTest;
import dev.langchain4j.cdi.mcp.integrationtests.McpHttpTransport;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@QuarkusTest
class McpQuarkusIntegrationTest extends AbstractMcpIntegrationTest {

    @ConfigProperty(name = "quarkus.http.test-port")
    int port;

    @Override
    protected McpHttpTransport transport() {
        return new RestAssuredTransport(port);
    }
}
