package dev.langchain4j.cdi.mcp.server.schema;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.cdi.mcp.server.fixtures.CalculatorTool;
import dev.langchain4j.cdi.mcp.server.fixtures.WeatherTool;
import jakarta.json.JsonObject;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class JsonSchemaGeneratorTest {

    @Test
    void shouldGenerateSchemaForStringParams() throws Exception {
        Method method = WeatherTool.class.getMethod("getWeather", String.class, String.class);
        JsonObject schema = JsonSchemaGenerator.fromMethod(method);

        assertThat(schema.getString("type")).isEqualTo("object");
        assertThat(schema.getJsonObject("properties")).isNotNull();
        assertThat(schema.getJsonObject("properties").size()).isEqualTo(2);
        assertThat(schema.getJsonArray("required").size()).isEqualTo(2);

        // Check each property has type "string"
        schema.getJsonObject("properties").values().forEach(val -> {
            JsonObject prop = val.asJsonObject();
            assertThat(prop.getString("type")).isEqualTo("string");
            assertThat(prop.containsKey("description")).isTrue();
        });
    }

    @Test
    void shouldGenerateSchemaForIntParams() throws Exception {
        Method method = CalculatorTool.class.getMethod("add", int.class, int.class);
        JsonObject schema = JsonSchemaGenerator.fromMethod(method);

        assertThat(schema.getString("type")).isEqualTo("object");
        schema.getJsonObject("properties").values().forEach(val -> {
            assertThat(val.asJsonObject().getString("type")).isEqualTo("integer");
        });
    }

    @Test
    void shouldGenerateSchemaForDoubleParams() throws Exception {
        Method method = CalculatorTool.class.getMethod("multiply", double.class, double.class);
        JsonObject schema = JsonSchemaGenerator.fromMethod(method);

        schema.getJsonObject("properties").values().forEach(val -> {
            assertThat(val.asJsonObject().getString("type")).isEqualTo("number");
        });
    }

    @Test
    void shouldIncludeDescriptionFromPAnnotation() throws Exception {
        Method method = WeatherTool.class.getMethod("getWeather", String.class, String.class);
        JsonObject schema = JsonSchemaGenerator.fromMethod(method);

        // At least one property should have a non-empty description
        boolean hasDescription = schema.getJsonObject("properties").values().stream()
                .anyMatch(
                        val -> !val.asJsonObject().getString("description", "").isEmpty());
        assertThat(hasDescription).isTrue();
    }
}
