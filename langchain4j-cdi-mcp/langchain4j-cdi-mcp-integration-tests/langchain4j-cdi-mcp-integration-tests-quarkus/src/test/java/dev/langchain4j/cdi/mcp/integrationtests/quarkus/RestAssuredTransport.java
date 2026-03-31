package dev.langchain4j.cdi.mcp.integrationtests.quarkus;

import static io.restassured.RestAssured.given;

import dev.langchain4j.cdi.mcp.integrationtests.McpHttpResponse;
import dev.langchain4j.cdi.mcp.integrationtests.McpHttpTransport;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;

class RestAssuredTransport implements McpHttpTransport {

    private final int port;

    RestAssuredTransport(int port) {
        this.port = port;
    }

    @Override
    public McpHttpResponse post(String path, String body, Map<String, String> headers) {
        var spec = given().contentType("application/json").body(body);
        headers.forEach(spec::header);
        Response response = spec.post(path);
        return toResponse(response);
    }

    @Override
    public McpHttpResponse delete(String path, Map<String, String> headers) {
        var spec = given();
        headers.forEach(spec::header);
        Response response = spec.delete(path);
        return toResponse(response);
    }

    @Override
    public String baseUrl() {
        return "http://localhost:" + port;
    }

    private McpHttpResponse toResponse(Response response) {
        Map<String, String> headers = new HashMap<>();
        response.headers().asList().forEach(h -> headers.putIfAbsent(h.getName(), h.getValue()));
        return new McpHttpResponse(response.statusCode(), response.body().asString(), headers);
    }
}
