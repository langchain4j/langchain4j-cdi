package dev.langchain4j.cdi.mcp.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;

public final class JsonRpcAssertions {

    static final String FIELD_ERROR = "error";
    static final String FIELD_RESULT = "result";

    private JsonRpcAssertions() {}

    public static JsonObject parseJson(McpHttpResponse response) {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isNotNull().isNotBlank();
        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            return reader.readObject();
        }
    }

    public static JsonObject assertJsonRpcSuccess(McpHttpResponse response, Object expectedId) {
        JsonObject json = parseJson(response);
        assertThat(json.getString("jsonrpc")).isEqualTo("2.0");
        assertJsonRpcIdMatches(json, expectedId);
        assertThat(json)
                .as("Expected 'result' field in response: %s", response.body())
                .containsKey(FIELD_RESULT);
        assertThat(json)
                .as("Unexpected 'error' field in response: %s", response.body())
                .doesNotContainKey(FIELD_ERROR);
        return json.getJsonObject(FIELD_RESULT);
    }

    public static JsonObject assertJsonRpcError(
            McpHttpResponse response, Object expectedId, int expectedErrorCode, String expectedMessageSubstring) {
        JsonObject json = parseJson(response);
        assertThat(json.getString("jsonrpc")).isEqualTo("2.0");
        assertJsonRpcIdMatches(json, expectedId);
        assertThat(json)
                .as("Expected 'error' field in response: %s", response.body())
                .containsKey(FIELD_ERROR);
        assertThat(json)
                .as("Unexpected 'result' field in error response: %s", response.body())
                .doesNotContainKey(FIELD_RESULT);

        JsonObject error = json.getJsonObject(FIELD_ERROR);
        assertThat(error.getInt("code")).isEqualTo(expectedErrorCode);
        if (expectedMessageSubstring != null) {
            assertThat(error.getString("message")).contains(expectedMessageSubstring);
        }
        return error;
    }

    public static void assertNotificationAccepted(McpHttpResponse response) {
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private static void assertJsonRpcIdMatches(JsonObject json, Object expectedId) {
        if (expectedId instanceof Integer intId) {
            assertThat(json.getInt("id")).isEqualTo(intId);
        } else if (expectedId instanceof String strId) {
            assertThat(json.getString("id")).isEqualTo(strId);
        } else if (expectedId != null) {
            fail(
                    "Unsupported id type: %s. Expected Integer or String.",
                    expectedId.getClass().getName());
        }
    }
}
