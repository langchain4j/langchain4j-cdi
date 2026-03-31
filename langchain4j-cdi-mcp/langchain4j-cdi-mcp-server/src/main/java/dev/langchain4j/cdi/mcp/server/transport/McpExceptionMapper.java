package dev.langchain4j.cdi.mcp.server.transport;

import dev.langchain4j.cdi.mcp.server.error.McpException;
import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcError;
import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcResponse;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class McpExceptionMapper implements ExceptionMapper<McpException> {

    @Override
    public Response toResponse(McpException e) {
        JsonRpcResponse errorResponse = JsonRpcResponse.error(
                e.getRequestId(), new JsonRpcError(e.getErrorCode().getCode(), e.getMessage()));
        JsonbConfig config = new JsonbConfig().withNullValues(false);
        try (Jsonb jsonb = JsonbBuilder.create(config)) {
            String json = jsonb.toJson(errorResponse);
            return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
        } catch (Exception ex) {
            return Response.serverError().build();
        }
    }
}
