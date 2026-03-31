package dev.langchain4j.cdi.mcp.server.transport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Allows the server to request LLM sampling from a connected client. Sends a {@code sampling/createMessage} request via
 * SSE and waits for the client's response.
 */
@ApplicationScoped
public class McpSamplingManager {

    private static final Logger LOGGER = Logger.getLogger(McpSamplingManager.class.getName());

    @Inject
    McpServerRequestManager requestManager;

    /**
     * Requests the client to create a message using its LLM.
     *
     * @param sessionId the target session
     * @param messages the conversation messages (list of maps with "role" and "content")
     * @param modelPreferences optional model preferences (can be null)
     * @param maxTokens maximum tokens in the response
     * @return the client's response as a JsonObject, or null on timeout/error
     */
    public JsonObject createMessage(
            String sessionId, List<Map<String, Object>> messages, Map<String, Object> modelPreferences, int maxTokens) {

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("messages", messages);
        if (modelPreferences != null) {
            params.put("modelPreferences", modelPreferences);
        }
        params.put("maxTokens", maxTokens);

        JsonObject result = requestManager.sendRequest(sessionId, "sampling/createMessage", params);
        if (result != null) {
            LOGGER.fine("MCP: Received sampling response from session " + sessionId);
        }
        return result;
    }
}
