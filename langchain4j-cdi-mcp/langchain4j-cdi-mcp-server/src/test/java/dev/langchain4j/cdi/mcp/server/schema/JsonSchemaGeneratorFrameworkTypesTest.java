package dev.langchain4j.cdi.mcp.server.schema;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.cdi.mcp.server.fixtures.LoggingTool;
import jakarta.json.JsonObject;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.mcp_java.server.McpLog;
import org.mcp_java.server.Progress;

class JsonSchemaGeneratorFrameworkTypesTest {

    @Test
    void shouldExcludeFrameworkTypesFromSchema() throws Exception {
        Method method = LoggingTool.class.getMethod("doWork", String.class, McpLog.class, Progress.class);
        JsonObject schema = JsonSchemaGenerator.fromMethod(method);

        assertThat(schema.getString("type")).isEqualTo("object");
        JsonObject properties = schema.getJsonObject("properties");

        // Only the 'input' parameter should be in the schema, not McpLog or Progress
        assertThat(properties.size()).isEqualTo(1);
        assertThat(properties.containsKey("input")).isTrue();
        assertThat(schema.getJsonArray("required").size()).isEqualTo(1);
    }

    @Test
    void shouldGenerateNormalSchemaForNonFrameworkTypes() throws Exception {
        Method method = LoggingTool.class.getMethod("simpleTool", String.class);
        JsonObject schema = JsonSchemaGenerator.fromMethod(method);

        JsonObject properties = schema.getJsonObject("properties");
        assertThat(properties.size()).isEqualTo(1);
        assertThat(properties.containsKey("name")).isTrue();
    }
}
