package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.transport.McpElicitationManager;
import jakarta.json.JsonObject;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.mcp_java.server.ElicitationRequest;
import org.mcp_java.server.ElicitationResponse;

/** Implementation of {@link ElicitationRequest} that delegates to {@link McpElicitationManager}. */
public class CdiElicitationRequest implements ElicitationRequest {

    private final String message;
    private final Map<String, PrimitiveSchema> requestedSchema;
    private final McpElicitationManager elicitationManager;
    private final String sessionId;
    private final long timeoutSeconds;

    CdiElicitationRequest(
            String message,
            Map<String, PrimitiveSchema> requestedSchema,
            McpElicitationManager elicitationManager,
            String sessionId,
            long timeoutSeconds) {
        this.message = message;
        this.requestedSchema = requestedSchema;
        this.elicitationManager = elicitationManager;
        this.sessionId = sessionId;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public Map<String, PrimitiveSchema> requestedSchema() {
        return requestedSchema;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T send() {
        return (T) sendAndAwait();
    }

    @Override
    public ElicitationResponse sendAndAwait() {
        Map<String, Object> schemaMap = new LinkedHashMap<>();
        if (requestedSchema != null) {
            requestedSchema.forEach((key, schema) -> schemaMap.put(key, schema.asJson()));
        }

        JsonObject result = elicitationManager.createElicitation(sessionId, message, schemaMap, timeoutSeconds);
        if (result == null) {
            return null;
        }
        return new CdiElicitationResponse(result);
    }

    static class CdiBuilder implements ElicitationRequest.Builder {

        private final McpElicitationManager elicitationManager;
        private final String sessionId;
        private String message;
        private final Map<String, PrimitiveSchema> requestedSchema = new LinkedHashMap<>();
        private long timeoutSeconds = 30;

        CdiBuilder(McpElicitationManager elicitationManager, String sessionId) {
            this.elicitationManager = elicitationManager;
            this.sessionId = sessionId;
        }

        @Override
        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Builder addSchemaProperty(String name, PrimitiveSchema schema) {
            this.requestedSchema.put(name, schema);
            return this;
        }

        @Override
        public Builder setTimeout(Duration timeout) {
            this.timeoutSeconds = timeout.toSeconds();
            return this;
        }

        @Override
        public ElicitationRequest build() {
            return new CdiElicitationRequest(
                    message, Map.copyOf(requestedSchema), elicitationManager, sessionId, timeoutSeconds);
        }
    }
}
