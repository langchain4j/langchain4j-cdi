package dev.langchain4j.cdi.mcp.server.registry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mcp_java.annotations.resources.Resource;

class McpResourceDescriptorTest {

    @Resource(uri = "data://config", name = "Config", description = "App config", mimeType = "application/json")
    public String getConfig() {
        return "{}";
    }

    @Resource(uri = "data://status")
    public String getStatus() {
        return "ok";
    }

    @Test
    void shouldCreateFromAnnotatedMethod() throws Exception {
        McpResourceDescriptor descriptor =
                McpResourceDescriptor.fromMethod(getClass(), getClass().getMethod("getConfig"));

        assertThat(descriptor.getUri()).isEqualTo("data://config");
        assertThat(descriptor.getName()).isEqualTo("Config");
        assertThat(descriptor.getDescription()).isEqualTo("App config");
        assertThat(descriptor.getMimeType()).isEqualTo("application/json");
    }

    @Test
    void shouldUseDefaults() throws Exception {
        McpResourceDescriptor descriptor =
                McpResourceDescriptor.fromMethod(getClass(), getClass().getMethod("getStatus"));

        assertThat(descriptor.getUri()).isEqualTo("data://status");
        assertThat(descriptor.getName()).isEqualTo("getStatus");
        assertThat(descriptor.getDescription()).isEmpty();
        assertThat(descriptor.getMimeType()).isEqualTo("text/plain");
    }
}
