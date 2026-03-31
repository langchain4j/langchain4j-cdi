package dev.langchain4j.cdi.mcp.server.registry;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.cdi.mcp.server.fixtures.CalculatorTool;
import dev.langchain4j.cdi.mcp.server.fixtures.WeatherTool;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpToolRegistryTest {

    McpToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new McpToolRegistry();
    }

    @Test
    void shouldRegisterAndFindTool() throws Exception {
        Method method = WeatherTool.class.getMethod("getWeather", String.class, String.class);
        McpToolDescriptor descriptor = McpToolDescriptor.fromMethod(WeatherTool.class, method);

        registry.register(descriptor);

        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.findTool("getWeather")).isPresent();
        assertThat(registry.findTool("getWeather").get().getDescription())
                .isEqualTo("Get the current weather for a given city");
    }

    @Test
    void shouldListAllRegisteredTools() throws Exception {
        Method weather = WeatherTool.class.getMethod("getWeather", String.class, String.class);
        Method add = CalculatorTool.class.getMethod("add", int.class, int.class);
        Method multiply = CalculatorTool.class.getMethod("multiply", double.class, double.class);

        registry.register(McpToolDescriptor.fromMethod(WeatherTool.class, weather));
        registry.register(McpToolDescriptor.fromMethod(CalculatorTool.class, add));
        registry.register(McpToolDescriptor.fromMethod(CalculatorTool.class, multiply));

        assertThat(registry.size()).isEqualTo(3);
        assertThat(registry.listTools()).hasSize(3);
    }

    @Test
    void shouldReturnEmptyForUnknownTool() {
        assertThat(registry.findTool("nonexistent")).isEmpty();
    }

    @Test
    void shouldOverwriteOnDuplicateName() throws Exception {
        Method method = WeatherTool.class.getMethod("getWeather", String.class, String.class);
        McpToolDescriptor d1 = McpToolDescriptor.fromMethod(WeatherTool.class, method);
        McpToolDescriptor d2 = McpToolDescriptor.fromMethod(WeatherTool.class, method);

        registry.register(d1);
        registry.register(d2);

        assertThat(registry.size()).isEqualTo(1);
    }
}
