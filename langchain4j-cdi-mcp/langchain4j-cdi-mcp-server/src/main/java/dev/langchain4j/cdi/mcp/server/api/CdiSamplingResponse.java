package dev.langchain4j.cdi.mcp.server.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mcp_java.server.Role;
import org.mcp_java.server.content.SamplingMessageContentBlock;
import org.mcp_java.server.sampling.SamplingResponse;

/** Implementation of {@link SamplingResponse} backed by a JSON-RPC result. */
class CdiSamplingResponse implements SamplingResponse {

    private final String model;
    private final String stopReason;

    CdiSamplingResponse(String model, String stopReason) {
        this.model = model;
        this.stopReason = stopReason;
    }

    @Override
    public List<SamplingMessageContentBlock> content() {
        return List.of();
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public Role role() {
        return Role.ASSISTANT;
    }

    @Override
    public Optional<String> stopReason() {
        return Optional.ofNullable(stopReason);
    }

    @Override
    public Map<String, Object> metadata() {
        return Map.of();
    }
}
