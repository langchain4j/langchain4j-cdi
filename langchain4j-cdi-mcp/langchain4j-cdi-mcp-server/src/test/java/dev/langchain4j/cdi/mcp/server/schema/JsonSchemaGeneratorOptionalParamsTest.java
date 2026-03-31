package dev.langchain4j.cdi.mcp.server.schema;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.cdi.mcp.server.fixtures.GreetingTool;
import dev.langchain4j.cdi.mcp.server.fixtures.WeatherTool;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonSchemaGeneratorOptionalParamsTest {

    @Test
    void shouldMarkAllParamsAsRequiredByDefault() throws Exception {
        Method method = WeatherTool.class.getMethod("getWeather", String.class, String.class);
        JsonObject schema = JsonSchemaGenerator.fromMethod(method);

        JsonArray required = schema.getJsonArray("required");
        assertThat(required).hasSize(2);
    }

    @Test
    void shouldExcludeOptionalParamsFromRequired() throws Exception {
        Method method = GreetingTool.class.getMethod("greet", String.class, String.class);
        JsonObject schema = JsonSchemaGenerator.fromMethod(method);

        JsonArray required = schema.getJsonArray("required");
        List<String> requiredNames = required.stream()
                .map(v -> ((jakarta.json.JsonString) v).getString())
                .toList();

        // "name" is required, "style" is not
        assertThat(requiredNames).containsExactly("name");
        assertThat(requiredNames).doesNotContain("style");
    }

    @Test
    void shouldStillIncludeOptionalParamsInProperties() throws Exception {
        Method method = GreetingTool.class.getMethod("greet", String.class, String.class);
        JsonObject schema = JsonSchemaGenerator.fromMethod(method);

        JsonObject properties = schema.getJsonObject("properties");
        assertThat(properties).hasSize(2);
        assertThat(properties.containsKey("name")).isTrue();
        assertThat(properties.containsKey("style")).isTrue();
    }

    @Test
    void shouldIncludeDescriptionForOptionalParams() throws Exception {
        Method method = GreetingTool.class.getMethod("greet", String.class, String.class);
        JsonObject schema = JsonSchemaGenerator.fromMethod(method);

        JsonObject styleProperty = schema.getJsonObject("properties").getJsonObject("style");
        assertThat(styleProperty.getString("description")).isEqualTo("Optional greeting style");
    }
}
