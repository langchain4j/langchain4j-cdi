package dev.langchain4j.cdi.mcp.integrationtests;

import java.util.Map;

/**
 * HTTP response from an MCP endpoint.
 *
 * @param statusCode the HTTP status code
 * @param body the response body
 * @param headers the response headers
 */
public record McpHttpResponse(int statusCode, String body, Map<String, String> headers) {

    /**
     * Returns the value of the given header, trying an exact match first then a case-insensitive lookup.
     *
     * @param name the header name
     * @return the header value, or {@code null} if not found
     */
    public String header(String name) {
        String value = headers.get(name);
        if (value != null) {
            return value;
        }
        return headers.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
