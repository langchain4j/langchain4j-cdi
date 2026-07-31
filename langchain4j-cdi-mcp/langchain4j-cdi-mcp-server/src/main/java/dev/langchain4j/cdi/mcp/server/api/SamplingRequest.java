package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.protocol.McpModelPreferences;
import dev.langchain4j.cdi.mcp.server.protocol.McpSamplingMessage;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Represents an LLM sampling request sent to the MCP client. */
public interface SamplingRequest {

    /** Controls how much MCP server context is included in the sampling request. */
    enum IncludeContext {
        NONE,
        THIS_SERVER,
        ALL_SERVERS
    }

    long maxTokens();

    List<McpSamplingMessage> messages();

    List<String> stopSequences();

    String systemPrompt();

    BigDecimal temperature();

    IncludeContext includeContext();

    McpModelPreferences modelPreferences();

    Map<String, Object> metadata();

    <T> T send();

    SamplingResponse sendAndAwait();

    /** Builder for constructing {@link SamplingRequest} instances. */
    interface Builder {

        Builder addMessage(McpSamplingMessage message);

        Builder setMaxTokens(long maxTokens);

        Builder setTemperature(BigDecimal temperature);

        Builder setSystemPrompt(String systemPrompt);

        Builder setIncludeContext(SamplingRequest.IncludeContext includeContext);

        Builder setModelPreferences(McpModelPreferences modelPreferences);

        Builder setMetadata(Map<String, Object> metadata);

        Builder setStopSequences(List<String> stopSequences);

        Builder setTimeout(Duration timeout);

        SamplingRequest build();
    }
}
