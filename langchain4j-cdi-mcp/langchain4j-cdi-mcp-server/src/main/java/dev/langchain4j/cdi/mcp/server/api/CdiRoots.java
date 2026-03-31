package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.transport.McpRootsManager;
import dev.langchain4j.cdi.mcp.server.transport.McpSession;
import java.util.List;
import org.mcp_java.model.roots.Root;
import org.mcp_java.server.Roots;

/** Implementation of {@link Roots} that delegates to {@link McpRootsManager}. */
public class CdiRoots implements Roots {

    private final McpSession session;
    private final McpRootsManager rootsManager;
    private final String sessionId;

    public CdiRoots(McpSession session, McpRootsManager rootsManager, String sessionId) {
        this.session = session;
        this.rootsManager = rootsManager;
        this.sessionId = sessionId;
    }

    @Override
    public boolean isSupported() {
        return session.hasCapability("roots");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T list() {
        return (T) listAndAwait();
    }

    @Override
    public List<Root> listAndAwait() {
        return rootsManager.requestRoots(sessionId);
    }
}
