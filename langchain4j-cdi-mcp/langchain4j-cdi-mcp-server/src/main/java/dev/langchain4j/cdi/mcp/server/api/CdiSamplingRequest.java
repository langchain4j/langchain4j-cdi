package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.transport.McpSamplingManager;
import jakarta.json.JsonObject;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mcp_java.server.Role;
import org.mcp_java.server.content.SamplingMessageContentBlock;
import org.mcp_java.server.content.TextContent;
import org.mcp_java.server.sampling.ModelPreferences;
import org.mcp_java.server.sampling.SamplingMessage;
import org.mcp_java.server.sampling.SamplingRequest;
import org.mcp_java.server.sampling.SamplingResponse;

/** Implementation of {@link SamplingRequest} that delegates to {@link McpSamplingManager}. */
public class CdiSamplingRequest implements SamplingRequest {

    private final long maxTokens;
    private final List<SamplingMessage> messages;
    private final List<String> stopSequences;
    private final String systemPrompt;
    private final BigDecimal temperature;
    private final SamplingRequest.IncludeContext includeContext;
    private final ModelPreferences modelPreferences;
    private final Map<String, Object> meta;
    private final McpSamplingManager samplingManager;
    private final String sessionId;

    CdiSamplingRequest(
            long maxTokens,
            List<SamplingMessage> messages,
            List<String> stopSequences,
            String systemPrompt,
            BigDecimal temperature,
            SamplingRequest.IncludeContext includeContext,
            ModelPreferences modelPreferences,
            Map<String, Object> meta,
            McpSamplingManager samplingManager,
            String sessionId) {
        this.maxTokens = maxTokens;
        this.messages = messages;
        this.stopSequences = stopSequences;
        this.systemPrompt = systemPrompt;
        this.temperature = temperature;
        this.includeContext = includeContext;
        this.modelPreferences = modelPreferences;
        this.meta = meta;
        this.samplingManager = samplingManager;
        this.sessionId = sessionId;
    }

    @Override
    public long maxTokens() {
        return maxTokens;
    }

    @Override
    public List<SamplingMessage> messages() {
        return messages;
    }

    @Override
    public List<String> stopSequences() {
        return stopSequences;
    }

    @Override
    public Optional<String> systemPrompt() {
        return Optional.ofNullable(systemPrompt);
    }

    @Override
    public Optional<BigDecimal> temperature() {
        return Optional.ofNullable(temperature);
    }

    @Override
    public Optional<SamplingRequest.IncludeContext> includeContext() {
        return Optional.ofNullable(includeContext);
    }

    @Override
    public Optional<ModelPreferences> modelPreferences() {
        return Optional.ofNullable(modelPreferences);
    }

    @Override
    public Map<String, Object> metadata() {
        return meta;
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
                    List<SamplingMessageContentBlock> content = m.content();
                    if (!content.isEmpty() && content.get(0) instanceof TextContent tc) {
                        map.put("content", tc.text());
                    }
                    return map;
                })
                .toList();

        Map<String, Object> modelPrefsMap = null;
        if (modelPreferences != null) {
            modelPrefsMap = new LinkedHashMap<>();
            modelPrefsMap.put("names", modelPreferences.names());
        }

        JsonObject result = samplingManager.createMessage(sessionId, messageMaps, modelPrefsMap, (int) maxTokens);
        if (result == null) {
            return null;
        }

        String model = result.getString("model", null);
        String stopReason = result.getString("stopReason", null);
        return new CdiSamplingResponse(model, stopReason);
    }

    static class CdiBuilder implements SamplingRequest.Builder {

        private final McpSamplingManager samplingManager;
        private final String sessionId;
        private long maxTokens = 1024;
        private final List<SamplingMessage> messages = new ArrayList<>();
        private List<String> stopSequences = List.of();
        private String systemPrompt;
        private BigDecimal temperature;
        private SamplingRequest.IncludeContext includeContext;
        private ModelPreferences modelPreferences;
        private Map<String, Object> meta = Map.of();

        CdiBuilder(McpSamplingManager samplingManager, String sessionId) {
            this.samplingManager = samplingManager;
            this.sessionId = sessionId;
        }

        @Override
        public Builder addMessage(Role role, SamplingMessageContentBlock... content) {
            List<SamplingMessageContentBlock> contentList = Arrays.asList(content);
            messages.add(new SamplingMessage() {
                @Override
                public Role role() {
                    return role;
                }

                @Override
                public List<SamplingMessageContentBlock> content() {
                    return contentList;
                }

                @Override
                public Map<String, Object> metadata() {
                    return Map.of();
                }
            });
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
        public Builder setIncludeContext(SamplingRequest.IncludeContext includeContext) {
            this.includeContext = includeContext;
            return this;
        }

        @Override
        public Builder setModelPreferences(ModelPreferences modelPreferences) {
            this.modelPreferences = modelPreferences;
            return this;
        }

        @Override
        public Builder setStopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
            return this;
        }

        @Override
        public Builder setTimeout(Duration timeout) {
            return this;
        }

        @Override
        public Builder putMetadata(String key, Object value) {
            Map<String, Object> updated = new LinkedHashMap<>(meta);
            updated.put(key, value);
            this.meta = updated;
            return this;
        }

        @Override
        public Builder setMetadata(Map<String, Object> metadata) {
            this.meta = metadata;
            return this;
        }

        @Override
        public SamplingRequest build() {
            return new CdiSamplingRequest(
                    maxTokens,
                    List.copyOf(messages),
                    stopSequences,
                    systemPrompt,
                    temperature,
                    includeContext,
                    modelPreferences,
                    meta,
                    samplingManager,
                    sessionId);
        }
    }
}
