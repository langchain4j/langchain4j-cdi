package dev.langchain4j.cdi.mcp.server.registry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mcp_java.annotations.resources.Resource;

class McpResourceRegistryTest {

    @Resource(uri = "test://data", name = "Test Data", description = "A test resource", mimeType = "text/plain")
    public String testResource() {
        return "test";
    }

    @Test
    void shouldRegisterAndFindResource() throws Exception {
        McpResourceRegistry registry = new McpResourceRegistry();
        McpResourceDescriptor descriptor =
                McpResourceDescriptor.fromMethod(getClass(), getClass().getMethod("testResource"));

        registry.register(descriptor);

        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.findResource("test://data")).isPresent();
        assertThat(registry.findResource("test://data").get().getName()).isEqualTo("Test Data");
        assertThat(registry.findResource("unknown://uri")).isEmpty();
    }

    @Test
    void shouldListResources() throws Exception {
        McpResourceRegistry registry = new McpResourceRegistry();
        McpResourceDescriptor descriptor =
                McpResourceDescriptor.fromMethod(getClass(), getClass().getMethod("testResource"));
        registry.register(descriptor);

        assertThat(registry.listResources()).hasSize(1);
    }
}
