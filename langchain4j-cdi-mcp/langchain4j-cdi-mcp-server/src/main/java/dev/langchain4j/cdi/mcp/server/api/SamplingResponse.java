package dev.langchain4j.cdi.mcp.server.api;

/** Holds the result of an LLM sampling request. */
public record SamplingResponse(Object content, String model, Object role, String stopReason) {}
