package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.transport.McpElicitationManager;
import dev.langchain4j.cdi.mcp.server.transport.McpSession;
import org.mcp_java.server.Elicitation;
import org.mcp_java.server.ElicitationRequest;

/** Implementation of {@link Elicitation} that delegates to {@link McpElicitationManager}. */
public class CdiElicitation implements Elicitation {

    private final McpSession session;
    private final McpElicitationManager elicitationManager;
    private final String sessionId;

    public CdiElicitation(McpSession session, McpElicitationManager elicitationManager, String sessionId) {
        this.session = session;
        this.elicitationManager = elicitationManager;
        this.sessionId = sessionId;
    }

    @Override
    public boolean isSupported() {
        return session.hasCapability("elicitation");
    }

    @Override
    public ElicitationRequest.Builder requestBuilder() {
        return new CdiElicitationRequest.CdiBuilder(elicitationManager, sessionId);
    }
}
