/** */
package dev.langchain4j.cdi.telemetry;

/**
 * Creates metrics that follow the <a
 * href="https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-agent-spans/">Semantic Conventions for GenAI agent
 * and framework spans</a>.
 *
 * @author Buhake Sindi
 * @since 21 June 2026
 */
public enum GenAIOperations {
    /** Chat completion operation such as OpenAI Chat API. */
    CHAT("chat", "Chat completion operation such as OpenAI Chat API"),
    /** Create a GenAI agent. */
    CREATE_AGENT("create_agent", "Create GenAI agent"),
    /** Create new memory records. */
    CREATE_MEMORY("create_memory", "Create new memory records"),
    /** Create or initialize a memory store. */
    CREATE_MEMORY_STORE("create_memory_store", "Create or initialize a memory store"),
    /** Delete memory records. */
    DELETE_MEMORY("delete_memory", "Delete memory records"),
    /** Delete or deprovision a memory store. */
    DELETE_MEMORY_STORE("delete_memory_store", "Delete or deprovision a memory store"),
    /** Embeddings operation such as OpenAI Create embeddings API. */
    EMBEDDINGS("embeddings", "Embeddings operation such as OpenAI Create embeddings API"),
    /** Execute a tool. */
    EXECUTE_TOOL("execute_tool", "Execute a tool"),
    /** Multimodal content generation operation such as Gemini Generate Content. */
    GENERATE_CONTENT("generate_content", "Multimodal content generation operation such as Gemini Generate Content"),
    /** Invoke a GenAI agent. */
    INVOKE_AGENT("invoke_agent", "Invoke GenAI agent"),
    /** Invoke a GenAI workflow. */
    INVOKE_WORKFLOW("invoke_workflow", "Invoke GenAI workflow"),
    /** Agent planning or task decomposition phase. */
    PLAN("plan", "Agent planning or task decomposition phase"),
    /** Retrieval operation such as OpenAI Search Vector Store API. */
    RETRIEVAL("retrieval", "Retrieval operation such as OpenAI Search Vector Store API"),
    /** Search or query memories from a memory store. */
    SEARCH_MEMORY("search_memory", "Search/query memories from a memory store"),
    /** Text completions operation such as OpenAI Completions API (Legacy). */
    TEXT_COMPLETION("text_completion", "Text completions operation such as OpenAI Completions API (Legacy)"),
    /** Update existing memory records. */
    UPDATE_MEMORY("update_memory", "Update existing memory records"),
    /** Create or update memory records without the caller choosing which. */
    UPSERT_MEMORY("upsert_memory", "Create or update memory records without the caller choosing which");

    private final String operationName;
    private final String description;

    /**
     * Creates a GenAI operation constant.
     *
     * @param operationName the OpenTelemetry operation name
     * @param description a human-readable description of the operation
     */
    private GenAIOperations(final String operationName, final String description) {
        this.operationName = operationName;
        this.description = description;
    }

    /**
     * Returns the human-readable description of this operation.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return operationName;
    }
}
