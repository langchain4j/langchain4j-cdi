package dev.langchain4j.cdi.mcp.integrationtests;

import java.util.Map;

/** HTTP transport abstraction for sending requests to an MCP endpoint. */
public interface McpHttpTransport {

    /**
     * Sends a POST request.
     *
     * @param path the request path
     * @param body the request body
     * @param headers additional headers
     * @return the response
     */
    McpHttpResponse post(String path, String body, Map<String, String> headers);

    /**
     * Sends a DELETE request.
     *
     * @param path the request path
     * @param headers additional headers
     * @return the response
     */
    McpHttpResponse delete(String path, Map<String, String> headers);

    /**
     * Returns the base URL of the MCP endpoint.
     *
     * @return the base URL
     */
    String baseUrl();
}
