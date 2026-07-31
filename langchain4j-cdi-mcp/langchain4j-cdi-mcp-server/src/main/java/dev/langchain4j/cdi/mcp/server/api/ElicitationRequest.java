package dev.langchain4j.cdi.mcp.server.api;

import java.time.Duration;
import java.util.Map;

/** Represents a user elicitation request sent to the MCP client. */
public interface ElicitationRequest {

    /** Schema descriptor for a single primitive property in the elicitation form. */
    interface PrimitiveSchema {
        Object asJson();
    }

    String message();

    Map<String, PrimitiveSchema> requestedSchema();

    <T> T send();

    ElicitationResponse sendAndAwait();

    /** Builder for constructing {@link ElicitationRequest} instances. */
    interface Builder {

        Builder setMessage(String message);

        Builder addSchemaProperty(String name, PrimitiveSchema schema);

        Builder setTimeout(Duration timeout);

        ElicitationRequest build();
    }
}
