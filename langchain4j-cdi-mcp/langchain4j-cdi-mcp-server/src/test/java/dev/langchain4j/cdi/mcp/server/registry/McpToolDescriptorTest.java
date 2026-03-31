package dev.langchain4j.cdi.mcp.server.registry;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.cdi.mcp.server.fixtures.WeatherTool;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.mcp_java.model.tool.Tool;

class McpToolDescriptorTest {

    @Test
    void shouldCreateFromMethod() throws Exception {
        Method method = WeatherTool.class.getMethod("getWeather", String.class, String.class);
        McpToolDescriptor descriptor = McpToolDescriptor.fromMethod(WeatherTool.class, method);

        assertThat(descriptor.getName()).isEqualTo("getWeather");
        assertThat(descriptor.getDescription()).isEqualTo("Get the current weather for a given city");
        assertThat(descriptor.getBeanType()).isEqualTo(WeatherTool.class);
        assertThat(descriptor.getMethod()).isEqualTo(method);
        assertThat(descriptor.getInputSchema()).isNotNull();
        assertThat(descriptor.getInputSchema().getString("type")).isEqualTo("object");
    }

    @Test
    void shouldConvertToWireFormat() throws Exception {
        Method method = WeatherTool.class.getMethod("getWeather", String.class, String.class);
        McpToolDescriptor descriptor = McpToolDescriptor.fromMethod(WeatherTool.class, method);

        Tool wire = descriptor.toWireFormat();

        assertThat(wire.name()).isEqualTo("getWeather");
        assertThat(wire.description()).isEqualTo("Get the current weather for a given city");
        assertThat(wire.inputSchema()).isNotNull();
    }
}
