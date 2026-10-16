/** */
package dev.langchain4j.cdi.telemetry.conventions;

/**
 * Enum representing the GenAI semantic convention attributes. Each constant holds the exact attribute key as defined in
 * the OpenTelemetry Semantic Conventions for GenAI.
 *
 * @author Buhake Sindi
 * @since 09 October 2026
 * @see <a
 *     href="https://github.com/open-telemetry/semantic-conventions-genai/blob/main/docs/registry/attributes/gen-ai.md">Gen
 *     AI attributes</a>.
 */
public enum GenAIAttributes {

    /** Agent description ({@code gen_ai.agent.description}). */
    AGENT_DESCRIPTION("gen_ai.agent.description"),

    /** Agent identifier ({@code gen_ai.agent.id}). */
    AGENT_ID("gen_ai.agent.id"),

    /** Agent name ({@code gen_ai.agent.name}). */
    AGENT_NAME("gen_ai.agent.name"),

    /** Agent version ({@code gen_ai.agent.version}). */
    AGENT_VERSION("gen_ai.agent.version"),

    /** Whether the conversation was compacted ({@code gen_ai.conversation.compacted}). */
    CONVERSATION_COMPACTED("gen_ai.conversation.compacted"),

    /** Conversation identifier ({@code gen_ai.conversation.id}). */
    CONVERSATION_ID("gen_ai.conversation.id"),

    /** Data source identifier ({@code gen_ai.data_source.id}). */
    DATA_SOURCE_ID("gen_ai.data_source.id"),

    /** Number of embedding dimensions ({@code gen_ai.embeddings.dimension.count}). */
    EMBEDDINGS_DIMENSION_COUNT("gen_ai.embeddings.dimension.count"),

    /** Evaluation explanation ({@code gen_ai.evaluation.explanation}). */
    EVALUATION_EXPLANATION("gen_ai.evaluation.explanation"),

    /** Evaluation name ({@code gen_ai.evaluation.name}). */
    EVALUATION_NAME("gen_ai.evaluation.name"),

    /** Evaluation score label ({@code gen_ai.evaluation.score.label}). */
    EVALUATION_SCORE_LABEL("gen_ai.evaluation.score.label"),

    /** Evaluation score value ({@code gen_ai.evaluation.score.value}). */
    EVALUATION_SCORE_VALUE("gen_ai.evaluation.score.value"),

    /** Input messages ({@code gen_ai.input.messages}). */
    INPUT_MESSAGES("gen_ai.input.messages"),

    /** Main agent description ({@code gen_ai.main_agent.description}). */
    MAIN_AGENT_DESCRIPTION("gen_ai.main_agent.description"),

    /** Main agent identifier ({@code gen_ai.main_agent.id}). */
    MAIN_AGENT_ID("gen_ai.main_agent.id"),

    /** Main agent name ({@code gen_ai.main_agent.name}). */
    MAIN_AGENT_NAME("gen_ai.main_agent.name"),

    /** Memory query text ({@code gen_ai.memory.query.text}). */
    MEMORY_QUERY_TEXT("gen_ai.memory.query.text"),

    /** Memory record count ({@code gen_ai.memory.record.count}). */
    MEMORY_RECORD_COUNT("gen_ai.memory.record.count"),

    /** Memory record identifier ({@code gen_ai.memory.record.id}). */
    MEMORY_RECORD_ID("gen_ai.memory.record.id"),

    /** Memory records ({@code gen_ai.memory.records}). */
    MEMORY_RECORDS("gen_ai.memory.records"),

    /** Memory store identifier ({@code gen_ai.memory.store.id}). */
    MEMORY_STORE_ID("gen_ai.memory.store.id"),

    /** Operation name ({@code gen_ai.operation.name}). */
    OPERATION_NAME("gen_ai.operation.name"),

    /** Output messages ({@code gen_ai.output.messages}). */
    OUTPUT_MESSAGES("gen_ai.output.messages"),

    /** Output type ({@code gen_ai.output.type}). */
    OUTPUT_TYPE("gen_ai.output.type"),

    /** Prompt name ({@code gen_ai.prompt.name}). */
    PROMPT_NAME("gen_ai.prompt.name"),

    /** Prompt variable ({@code gen_ai.prompt.variable}). */
    PROMPT_VARIABLE("gen_ai.prompt.variable"),

    /** Prompt version ({@code gen_ai.prompt.version}). */
    PROMPT_VERSION("gen_ai.prompt.version"),

    /** Provider name ({@code gen_ai.provider.name}). */
    PROVIDER_NAME("gen_ai.provider.name"),

    /** Request choice count ({@code gen_ai.request.choice.count}). */
    REQUEST_CHOICE_COUNT("gen_ai.request.choice.count"),

    /** Request encoding formats ({@code gen_ai.request.encoding_formats}). */
    REQUEST_ENCODING_FORMATS("gen_ai.request.encoding_formats"),

    /** Request frequency penalty ({@code gen_ai.request.frequency_penalty}). */
    REQUEST_FREQUENCY_PENALTY("gen_ai.request.frequency_penalty"),

    /** Request maximum tokens ({@code gen_ai.request.max_tokens}). */
    REQUEST_MAX_TOKENS("gen_ai.request.max_tokens"),

    /** Request model ({@code gen_ai.request.model}). */
    REQUEST_MODEL("gen_ai.request.model"),

    /** Request presence penalty ({@code gen_ai.request.presence_penalty}). */
    REQUEST_PRESENCE_PENALTY("gen_ai.request.presence_penalty"),

    /** Previous response identifier ({@code gen_ai.request.previous_response.id}). */
    REQUEST_PREVIOUS_RESPONSE_ID("gen_ai.request.previous_response.id"),

    /** Request reasoning level ({@code gen_ai.request.reasoning.level}). */
    REQUEST_REASONING_LEVEL("gen_ai.request.reasoning.level"),

    /** Request seed ({@code gen_ai.request.seed}). */
    REQUEST_SEED("gen_ai.request.seed"),

    /** Request stop sequences ({@code gen_ai.request.stop_sequences}). */
    REQUEST_STOP_SEQUENCES("gen_ai.request.stop_sequences"),

    /** Whether the request is streaming ({@code gen_ai.request.stream}). */
    REQUEST_STREAM("gen_ai.request.stream"),

    /** Request stream cursor ({@code gen_ai.request.stream_cursor}). */
    REQUEST_STREAM_CURSOR("gen_ai.request.stream_cursor"),

    /** Request temperature ({@code gen_ai.request.temperature}). */
    REQUEST_TEMPERATURE("gen_ai.request.temperature"),

    /** Request top-k ({@code gen_ai.request.top_k}). */
    REQUEST_TOP_K("gen_ai.request.top_k"),

    /** Request top-p ({@code gen_ai.request.top_p}). */
    REQUEST_TOP_P("gen_ai.request.top_p"),

    /** Response finish reasons ({@code gen_ai.response.finish_reasons}). */
    RESPONSE_FINISH_REASONS("gen_ai.response.finish_reasons"),

    /** Response identifier ({@code gen_ai.response.id}). */
    RESPONSE_ID("gen_ai.response.id"),

    /** Response model ({@code gen_ai.response.model}). */
    RESPONSE_MODEL("gen_ai.response.model"),

    /** Response status ({@code gen_ai.response.status}). */
    RESPONSE_STATUS("gen_ai.response.status"),

    /** Time to first chunk in a streaming response ({@code gen_ai.response.time_to_first_chunk}). */
    RESPONSE_TIME_TO_FIRST_CHUNK("gen_ai.response.time_to_first_chunk"),

    /** Retrieval documents ({@code gen_ai.retrieval.documents}). */
    RETRIEVAL_DOCUMENTS("gen_ai.retrieval.documents"),

    /** Retrieval query text ({@code gen_ai.retrieval.query.text}). */
    RETRIEVAL_QUERY_TEXT("gen_ai.retrieval.query.text"),

    /** Retrieval top-k ({@code gen_ai.retrieval.top_k}). */
    RETRIEVAL_TOP_K("gen_ai.retrieval.top_k"),

    /** Skill description ({@code gen_ai.skill.description}). */
    SKILL_DESCRIPTION("gen_ai.skill.description"),

    /** Skill name ({@code gen_ai.skill.name}). */
    SKILL_NAME("gen_ai.skill.name"),

    /** Skill resource name ({@code gen_ai.skill.resource.name}). */
    SKILL_RESOURCE_NAME("gen_ai.skill.resource.name"),

    /** Skill source URI ({@code gen_ai.skill.source.uri}). */
    SKILL_SOURCE_URI("gen_ai.skill.source.uri"),

    /** System instructions ({@code gen_ai.system_instructions}). */
    SYSTEM_INSTRUCTIONS("gen_ai.system_instructions"),

    /** Token modality ({@code gen_ai.token.modality}). */
    TOKEN_MODALITY("gen_ai.token.modality"),

    /** Tool call arguments ({@code gen_ai.tool.call.arguments}). */
    TOOL_CALL_ARGUMENTS("gen_ai.tool.call.arguments"),

    /** Tool call identifier ({@code gen_ai.tool.call.id}). */
    TOOL_CALL_ID("gen_ai.tool.call.id"),

    /** Tool call result ({@code gen_ai.tool.call.result}). */
    TOOL_CALL_RESULT("gen_ai.tool.call.result"),

    /** Tool definitions ({@code gen_ai.tool.definitions}). */
    TOOL_DEFINITIONS("gen_ai.tool.definitions"),

    /** Tool description ({@code gen_ai.tool.description}). */
    TOOL_DESCRIPTION("gen_ai.tool.description"),

    /** Tool name ({@code gen_ai.tool.name}). */
    TOOL_NAME("gen_ai.tool.name"),

    /** Tool type ({@code gen_ai.tool.type}). */
    TOOL_TYPE("gen_ai.tool.type"),

    /** Audio cache-read input tokens ({@code gen_ai.usage.audio.cache_read.input_tokens}). */
    USAGE_AUDIO_CACHE_READ_INPUT_TOKENS("gen_ai.usage.audio.cache_read.input_tokens"),

    /** Audio input tokens ({@code gen_ai.usage.audio.input_tokens}). */
    USAGE_AUDIO_INPUT_TOKENS("gen_ai.usage.audio.input_tokens"),

    /** Audio output tokens ({@code gen_ai.usage.audio.output_tokens}). */
    USAGE_AUDIO_OUTPUT_TOKENS("gen_ai.usage.audio.output_tokens"),

    /** Cache-read input tokens ({@code gen_ai.usage.cache_read.input_tokens}). */
    USAGE_CACHE_READ_INPUT_TOKENS("gen_ai.usage.cache_read.input_tokens"),

    /** Cache-write input tokens ({@code gen_ai.usage.cache_write.input_tokens}). */
    USAGE_CACHE_WRITE_INPUT_TOKENS("gen_ai.usage.cache_write.input_tokens"),

    /** Image cache-read input tokens ({@code gen_ai.usage.image.cache_read.input_tokens}). */
    USAGE_IMAGE_CACHE_READ_INPUT_TOKENS("gen_ai.usage.image.cache_read.input_tokens"),

    /** Image input tokens ({@code gen_ai.usage.image.input_tokens}). */
    USAGE_IMAGE_INPUT_TOKENS("gen_ai.usage.image.input_tokens"),

    /** Image output tokens ({@code gen_ai.usage.image.output_tokens}). */
    USAGE_IMAGE_OUTPUT_TOKENS("gen_ai.usage.image.output_tokens"),

    /** Input tokens ({@code gen_ai.usage.input_tokens}). */
    USAGE_INPUT_TOKENS("gen_ai.usage.input_tokens"),

    /** Output tokens ({@code gen_ai.usage.output_tokens}). */
    USAGE_OUTPUT_TOKENS("gen_ai.usage.output_tokens"),

    /** Reasoning output tokens ({@code gen_ai.usage.reasoning.output_tokens}). */
    USAGE_REASONING_OUTPUT_TOKENS("gen_ai.usage.reasoning.output_tokens"),

    /** Text cache-read input tokens ({@code gen_ai.usage.text.cache_read.input_tokens}). */
    USAGE_TEXT_CACHE_READ_INPUT_TOKENS("gen_ai.usage.text.cache_read.input_tokens"),

    /** Text input tokens ({@code gen_ai.usage.text.input_tokens}). */
    USAGE_TEXT_INPUT_TOKENS("gen_ai.usage.text.input_tokens"),

    /** Text output tokens ({@code gen_ai.usage.text.output_tokens}). */
    USAGE_TEXT_OUTPUT_TOKENS("gen_ai.usage.text.output_tokens"),

    /** Workflow name ({@code gen_ai.workflow.name}). */
    WORKFLOW_NAME("gen_ai.workflow.name"),

    /** The type of error that occurred ({@code error.type}). */
    ERROR_TYPE("error.type"),

    /** The GenAI server port ({@code server.port}). */
    SERVER_PORT("server.port"),

    /** The GenAI server address ({@code server.address}). */
    SERVER_ADDRESS("server.address");

    private final String value;

    /** @param value the OpenTelemetry semantic convention attribute key */
    private GenAIAttributes(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
