package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.transport.McpSamplingManager;
import dev.langchain4j.cdi.mcp.server.transport.McpSession;
import org.mcp_java.server.Sampling;
import org.mcp_java.server.SamplingRequest;

/** Implementation of {@link Sampling} that delegates to {@link McpSamplingManager}. */
public class CdiSampling implements Sampling {

    private final McpSession session;
    private final McpSamplingManager samplingManager;
    private final String sessionId;

    public CdiSampling(McpSession session, McpSamplingManager samplingManager, String sessionId) {
        this.session = session;
        this.samplingManager = samplingManager;
        this.sessionId = sessionId;
    }

    @Override
    public boolean isSupported() {
        return session.hasCapability("sampling");
    }

    @Override
    public SamplingRequest.Builder requestBuilder() {
        return new CdiSamplingRequest.CdiBuilder(samplingManager, sessionId);
    }
}
