package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.protocol.McpModelPreferences;
import dev.langchain4j.cdi.mcp.server.protocol.McpSamplingMessage;
import dev.langchain4j.cdi.mcp.server.transport.McpSamplingManager;
import jakarta.json.JsonObject;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Implementation of {@link SamplingRequest} that delegates to {@link McpSamplingManager}. */
public class CdiSamplingRequest implements SamplingRequest {

    private final long maxTokens;
    private final List<McpSamplingMessage> messages;
    private final List<String> stopSequences;
    private final String systemPrompt;
    private final BigDecimal temperature;
    private final IncludeContext includeContext;
    private final McpModelPreferences modelPreferences;
    private final Map<String, Object> metadata;
    private final McpSamplingManager samplingManager;
    private final String sessionId;

    CdiSamplingRequest(
            long maxTokens,
            List<McpSamplingMessage> messages,
            List<String> stopSequences,
            String systemPrompt,
            BigDecimal temperature,
            IncludeContext includeContext,
            McpModelPreferences modelPreferences,
            Map<String, Object> metadata,
            McpSamplingManager samplingManager,
            String sessionId) {
        this.maxTokens = maxTokens;
        this.messages = messages;
        this.stopSequences = stopSequences;
        this.systemPrompt = systemPrompt;
        this.temperature = temperature;
        this.includeContext = includeContext;
        this.modelPreferences = modelPreferences;
        this.metadata = metadata;
        this.samplingManager = samplingManager;
        this.sessionId = sessionId;
    }

    @Override
    public long maxTokens() {
        return maxTokens;
    }

    @Override
    public List<McpSamplingMessage> messages() {
        return messages;
    }

    @Override
    public List<String> stopSequences() {
        return stopSequences;
    }

    @Override
    public String systemPrompt() {
        return systemPrompt;
    }

    @Override
    public BigDecimal temperature() {
        return temperature;
    }

    @Override
    public IncludeContext includeContext() {
        return includeContext;
    }

    @Override
    public McpModelPreferences modelPreferences() {
        return modelPreferences;
    }

    @Override
    public Map<String, Object> metadata() {
        return metadata;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T send() {
        return (T) sendAndAwait();
    }

    @Override
    public SamplingResponse sendAndAwait() {
        List<Map<String, Object>> messageMaps = messages.stream()
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("role", m.role().toString());
                    map.put("content", m.content());
                    return map;
                })
                .toList();

        Map<String, Object> modelPrefsMap = null;
        if (modelPreferences != null) {
            modelPrefsMap = new LinkedHashMap<>();
            modelPrefsMap.put("hints", modelPreferences.hints());
        }

        JsonObject result = samplingManager.createMessage(sessionId, messageMaps, modelPrefsMap, (int) maxTokens);
        if (result == null) {
            return null;
        }

        return new SamplingResponse(null, result.getString("model", null), null, result.getString("stopReason", null));
    }

    static class CdiBuilder implements SamplingRequest.Builder {

        private final McpSamplingManager samplingManager;
        private final String sessionId;
        private long maxTokens = 1024;
        private final List<McpSamplingMessage> messages = new ArrayList<>();
        private List<String> stopSequences = List.of();
        private String systemPrompt;
        private BigDecimal temperature;
        private IncludeContext includeContext;
        private McpModelPreferences modelPreferences;
        private Map<String, Object> metadata = Map.of();

        CdiBuilder(McpSamplingManager samplingManager, String sessionId) {
            this.samplingManager = samplingManager;
            this.sessionId = sessionId;
        }

        @Override
        public Builder addMessage(McpSamplingMessage message) {
            messages.add(message);
            return this;
        }

        @Override
        public Builder setMaxTokens(long maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        @Override
        public Builder setTemperature(BigDecimal temperature) {
            this.temperature = temperature;
            return this;
        }

        @Override
        public Builder setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        @Override
        public Builder setIncludeContext(IncludeContext includeContext) {
            this.includeContext = includeContext;
            return this;
        }

        @Override
        public Builder setModelPreferences(McpModelPreferences modelPreferences) {
            this.modelPreferences = modelPreferences;
            return this;
        }

        @Override
        public Builder setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        @Override
        public Builder setStopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
            return this;
        }

        @Override
        public Builder setTimeout(Duration timeout) {
            // timeout is not directly used in our implementation
            return this;
        }

        @Override
        public SamplingRequest build() {
            if (messages.isEmpty()) {
                throw new IllegalStateException("At least one sampling message is required");
            }
            if (maxTokens <= 0) {
                throw new IllegalStateException("maxTokens must be positive");
            }
            return new CdiSamplingRequest(
                    maxTokens,
                    List.copyOf(messages),
                    List.copyOf(stopSequences),
                    systemPrompt,
                    temperature,
                    includeContext,
                    modelPreferences,
                    Map.copyOf(metadata),
                    samplingManager,
                    sessionId);
        }
    }
}
