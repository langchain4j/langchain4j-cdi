package dev.langchain4j.cdi.mcp.integrationtests.helidon;

import dev.langchain4j.cdi.mcp.integrationtests.McpHttpResponse;
import dev.langchain4j.cdi.mcp.integrationtests.McpHttpTransport;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

class HelidonWebTargetTransport implements McpHttpTransport {

    private final WebTarget target;

    HelidonWebTargetTransport(WebTarget target) {
        this.target = target;
    }

    @Override
    public McpHttpResponse post(String path, String body, Map<String, String> headers) {
        var builder = target.path(path).request(MediaType.APPLICATION_JSON);
        headers.forEach(builder::header);
        Response response = builder.post(Entity.json(body));
        return toResponse(response);
    }

    @Override
    public McpHttpResponse delete(String path, Map<String, String> headers) {
        var builder = target.path(path).request();
        headers.forEach(builder::header);
        Response response = builder.delete();
        return toResponse(response);
    }

    @Override
    public String baseUrl() {
        String uri = target.getUri().toString();
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }

    private McpHttpResponse toResponse(Response response) {
        Map<String, String> headers = new HashMap<>();
        response.getHeaders().forEach((k, v) -> {
            if (v != null && !v.isEmpty()) {
                headers.put(k, v.get(0).toString());
            }
        });
        return new McpHttpResponse(response.getStatus(), response.readEntity(String.class), headers);
    }
}
