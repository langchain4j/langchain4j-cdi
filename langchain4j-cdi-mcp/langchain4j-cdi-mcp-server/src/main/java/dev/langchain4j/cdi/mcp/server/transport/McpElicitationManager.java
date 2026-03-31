package dev.langchain4j.cdi.mcp.server.transport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Allows the server to request user input (elicitation) from a connected client. Sends an {@code elicitation/create}
 * request via SSE and waits for the client's response.
 */
@ApplicationScoped
public class McpElicitationManager {

    private static final Logger LOGGER = Logger.getLogger(McpElicitationManager.class.getName());

    @Inject
    McpServerRequestManager requestManager;

    /**
     * Sends an elicitation request to the client.
     *
     * @param sessionId the target session
     * @param message the message to display to the user
     * @param requestedSchema the JSON schema for the expected response
     * @param timeoutSeconds how long to wait for the response
     * @return the client's response as a JsonObject, or null on timeout/error
     */
    public JsonObject createElicitation(
            String sessionId, String message, Map<String, Object> requestedSchema, long timeoutSeconds) {

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("message", message);
        if (requestedSchema != null) {
            params.put("requestedSchema", requestedSchema);
        }

        JsonObject result = requestManager.sendRequest(sessionId, "elicitation/create", params, timeoutSeconds);
        if (result != null) {
            LOGGER.fine("MCP: Received elicitation response from session " + sessionId);
        }
        return result;
    }
}
