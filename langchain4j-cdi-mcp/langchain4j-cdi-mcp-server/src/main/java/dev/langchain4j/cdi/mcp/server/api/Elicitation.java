package dev.langchain4j.cdi.mcp.server.api;

/** Provides user elicitation capability via the MCP client. */
public interface Elicitation {

    boolean isSupported();

    ElicitationRequest.Builder requestBuilder();
}
