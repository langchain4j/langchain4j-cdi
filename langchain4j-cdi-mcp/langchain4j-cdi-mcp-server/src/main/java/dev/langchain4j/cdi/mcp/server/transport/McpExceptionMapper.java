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

/** JAX-RS exception mapper that converts {@link McpException} instances into JSON-RPC error responses. */
@Provider
public class McpExceptionMapper implements ExceptionMapper<McpException> {

    /** Creates a new exception mapper. */
    public McpExceptionMapper() {}

    /**
     * Converts an {@link McpException} into a JAX-RS {@link Response} containing a JSON-RPC error object.
     *
     * @param e the MCP exception to convert
     * @return a JSON response with the corresponding JSON-RPC error
     */
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
