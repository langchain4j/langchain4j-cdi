package dev.langchain4j.cdi.mcp.integrationtests;

import java.util.Map;

public interface McpHttpTransport {

    McpHttpResponse post(String path, String body, Map<String, String> headers);

    McpHttpResponse delete(String path, Map<String, String> headers);

    String baseUrl();
}
