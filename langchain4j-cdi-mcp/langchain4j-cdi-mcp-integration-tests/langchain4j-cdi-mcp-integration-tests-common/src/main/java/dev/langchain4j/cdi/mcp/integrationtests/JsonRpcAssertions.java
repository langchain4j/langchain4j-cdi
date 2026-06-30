package dev.langchain4j.cdi.mcp.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;

/** Assertion helpers for JSON-RPC responses in MCP integration tests. */
public final class JsonRpcAssertions {

    /** JSON field name for the error object. */
    static final String FIELD_ERROR = "error";

    /** JSON field name for the result object. */
    static final String FIELD_RESULT = "result";

    private JsonRpcAssertions() {}

    /**
     * Parses the response body as a {@link JsonObject}, asserting a 200 status.
     *
     * @param response the HTTP response
     * @return the parsed JSON object
     */
    public static JsonObject parseJson(McpHttpResponse response) {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isNotNull().isNotBlank();
        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            return reader.readObject();
        }
    }

    /**
     * Asserts a successful JSON-RPC response and returns its {@code result} object.
     *
     * @param response the HTTP response
     * @param expectedId the expected JSON-RPC id
     * @return the {@code result} JSON object
     */
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

    /**
     * Asserts a JSON-RPC error response and returns its {@code error} object.
     *
     * @param response the HTTP response
     * @param expectedId the expected JSON-RPC id
     * @param expectedErrorCode the expected error code
     * @param expectedMessageSubstring substring expected in the error message
     * @return the {@code error} JSON object
     */
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

    /**
     * Asserts that a notification was accepted (HTTP 200).
     *
     * @param response the HTTP response
     */
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
