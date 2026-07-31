package dev.langchain4j.cdi.mcp.server.api;

/** Provides LLM sampling capability via the MCP client. */
public interface Sampling {

    boolean isSupported();

    SamplingRequest.Builder requestBuilder();
}
