package dev.langchain4j.cdi.mcp.server.transport;

import dev.langchain4j.cdi.mcp.server.protocol.McpRoot;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages file roots reported by MCP clients. Roots represent the base directories or URIs that the client has made
 * available to the server.
 */
@ApplicationScoped
public class McpRootsManager {

    private static final Logger LOGGER = Logger.getLogger(McpRootsManager.class.getName());

    @Inject
    McpServerRequestManager requestManager;

    /** CDI-required default constructor. */
    public McpRootsManager() {}

    private final Map<String, List<McpRoot>> rootsBySession = new ConcurrentHashMap<>();

    /**
     * Requests the list of roots from a client session. Sends a {@code roots/list} request via SSE and waits for the
     * response.
     *
     * @param sessionId the target session
     * @return the list of roots, or empty list on timeout/error
     */
    public List<McpRoot> requestRoots(String sessionId) {
        JsonObject result = requestManager.sendRequest(sessionId, "roots/list", Map.of());
        if (result == null) {
            return Collections.emptyList();
        }

        List<McpRoot> roots = parseRoots(result);
        rootsBySession.put(sessionId, roots);
        LOGGER.info("MCP: Received " + roots.size() + " root(s) from session " + sessionId);
        return roots;
    }

    /**
     * Called when a client sends {@code notifications/roots/list_changed}. Re-requests the roots.
     *
     * @param sessionId the session whose roots changed
     */
    public void onRootsChanged(String sessionId) {
        requestRoots(sessionId);
    }

    /**
     * Returns the cached roots for a session.
     *
     * @param sessionId the session identifier
     * @return the list of roots, or empty list if none are cached
     */
    public List<McpRoot> getRoots(String sessionId) {
        return rootsBySession.getOrDefault(sessionId, Collections.emptyList());
    }

    /**
     * Removes cached roots for a session that has been terminated.
     *
     * @param sessionId the session identifier to remove
     */
    public void removeSession(String sessionId) {
        rootsBySession.remove(sessionId);
    }

    private List<McpRoot> parseRoots(JsonObject result) {
        if (!result.containsKey("roots")) {
            return Collections.emptyList();
        }
        JsonArray rootsArray = result.getJsonArray("roots");
        return rootsArray.stream()
                .map(v -> (JsonObject) v)
                .map(obj -> McpRoot.of(
                        obj.containsKey("uri") ? obj.getString("uri") : "",
                        obj.containsKey("name") ? obj.getString("name") : ""))
                .toList();
    }
}
