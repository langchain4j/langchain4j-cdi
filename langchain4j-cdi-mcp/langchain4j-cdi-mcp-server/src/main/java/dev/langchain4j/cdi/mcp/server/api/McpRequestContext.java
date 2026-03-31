package dev.langchain4j.cdi.mcp.server.api;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Immutable context carrying per-request information for MCP framework type injection.
 *
 * @param sessionId the MCP session identifier
 * @param requestId the JSON-RPC request identifier
 * @param progressToken the progress token from request _meta (may be null)
 * @param cancelledFlag shared flag set to true when the request is cancelled
 */
public record McpRequestContext(
        String sessionId, Object requestId, Object progressToken, AtomicBoolean cancelledFlag) {}
