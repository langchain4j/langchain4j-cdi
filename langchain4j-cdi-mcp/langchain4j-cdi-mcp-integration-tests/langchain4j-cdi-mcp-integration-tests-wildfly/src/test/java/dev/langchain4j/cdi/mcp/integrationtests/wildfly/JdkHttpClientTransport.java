package dev.langchain4j.cdi.mcp.integrationtests.wildfly;

import dev.langchain4j.cdi.mcp.integrationtests.McpHttpResponse;
import dev.langchain4j.cdi.mcp.integrationtests.McpHttpTransport;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

class JdkHttpClientTransport implements McpHttpTransport {

    private final String baseUrl;

    JdkHttpClientTransport(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public McpHttpResponse post(String path, String body, Map<String, String> headers) {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        headers.forEach(builder::header);
        return send(builder.build());
    }

    @Override
    public McpHttpResponse delete(String path, Map<String, String> headers) {
        var builder = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).DELETE();
        headers.forEach(builder::header);
        return send(builder.build());
    }

    @Override
    public String baseUrl() {
        return baseUrl;
    }

    private McpHttpResponse send(HttpRequest request) {
        try {
            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, String> headers = new HashMap<>();
            response.headers().map().forEach((k, v) -> {
                if (v != null && !v.isEmpty()) {
                    headers.put(k, v.get(0));
                }
            });
            return new McpHttpResponse(response.statusCode(), response.body(), headers);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
