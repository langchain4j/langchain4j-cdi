package dev.langchain4j.cdi.mcp.server.registry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mcp_java.annotations.resources.ResourceTemplate;

class McpResourceTemplateDescriptorTest {

    @SuppressWarnings("unused")
    static class TestBean {
        @ResourceTemplate(
                uriTemplate = "user://{userId}/profile",
                name = "User Profile",
                description = "Get user profile",
                mimeType = "application/json")
        public String getUserProfile(String userId) {
            return "{}";
        }

        @ResourceTemplate(uriTemplate = "file:///{path}")
        public String getFile(String path) {
            return "";
        }
    }

    @Test
    void shouldCreateFromMethodWithAllAttributes() throws Exception {
        var method = TestBean.class.getMethod("getUserProfile", String.class);
        McpResourceTemplateDescriptor descriptor = McpResourceTemplateDescriptor.fromMethod(TestBean.class, method);

        assertThat(descriptor.getUriTemplate()).isEqualTo("user://{userId}/profile");
        assertThat(descriptor.getName()).isEqualTo("User Profile");
        assertThat(descriptor.getDescription()).isEqualTo("Get user profile");
        assertThat(descriptor.getMimeType()).isEqualTo("application/json");
    }

    @Test
    void shouldDefaultNameToMethodName() throws Exception {
        var method = TestBean.class.getMethod("getFile", String.class);
        McpResourceTemplateDescriptor descriptor = McpResourceTemplateDescriptor.fromMethod(TestBean.class, method);

        assertThat(descriptor.getUriTemplate()).isEqualTo("file:///{path}");
        assertThat(descriptor.getName()).isEqualTo("getFile");
        assertThat(descriptor.getMimeType()).isEqualTo("text/plain");
    }
}
