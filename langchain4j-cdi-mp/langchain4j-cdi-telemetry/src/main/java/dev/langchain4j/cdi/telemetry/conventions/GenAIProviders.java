/** */
package dev.langchain4j.cdi.telemetry.conventions;

import dev.langchain4j.model.ModelProvider;

/**
 * Maps {@link ModelProvider} values to OpenTelemetry Semantic Conventions (v1.39.0) well-known values for the
 * {@code gen_ai.provider.name} attribute.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-spans/">OTel GenAI Spans</a>
 * @author Buhake Sindi
 * @since 09 October 2026
 */
public enum GenAIProviders {

    /** Anthropic ({@code anthropic}). */
    ANTHROPIC(ModelProvider.ANTHROPIC, "anthropic"),

    /** Amazon Bedrock ({@code aws.bedrock}). */
    AMAZON_BEDROCK(ModelProvider.AMAZON_BEDROCK, "aws.bedrock"),

    /** Azure OpenAI ({@code azure.ai.openai}), deprecated in favor of Microsoft Foundry. */
    @Deprecated
    AZURE_OPEN_AI(ModelProvider.AZURE_OPEN_AI, "azure.ai.openai"),

    /** Cohere ({@code cohere}). */
    COHERE(ModelProvider.COHERE, "cohere"),

    /** GitHub Models ({@code github}). */
    GITHUB_MODELS(ModelProvider.GITHUB_MODELS, "github"),

    /** Google AI Gemini ({@code gcp.gemini}). */
    GOOGLE_AI_GEMINI(ModelProvider.GOOGLE_AI_GEMINI, "gcp.gemini"),

    /** Google GenAI ({@code gcp.gen_ai}). */
    GOOGLE_GENAI(ModelProvider.GOOGLE_GENAI, "gcp.gen_ai"),

    /** Google Vertex AI Gemini ({@code gcp.vertex_ai}). */
    GOOGLE_VERTEX_AI_GEMINI(ModelProvider.GOOGLE_VERTEX_AI_GEMINI, "gcp.vertex_ai"),

    /** Google Vertex AI Anthropic ({@code gcp.vertex_ai}). */
    GOOGLE_VERTEX_AI_ANTHROPIC(ModelProvider.GOOGLE_VERTEX_AI_ANTHROPIC, "gcp.vertex_ai"),

    /** Jina ({@code jina}). */
    JINA(ModelProvider.JINA, "jina"),

    /** Microsoft Foundry ({@code azure.ai.inference}). */
    MICROSOFT_FOUNDRY(ModelProvider.MICROSOFT_FOUNDRY, "azure.ai.inference"),

    /** Mistral AI ({@code mistral_ai}). */
    MISTRAL_AI(ModelProvider.MISTRAL_AI, "mistral_ai"),

    /** Ollama ({@code ollama}). */
    OLLAMA(ModelProvider.OLLAMA, "ollama"),

    /** OpenAI ({@code openai}). */
    OPEN_AI(ModelProvider.OPEN_AI, "openai"),

    /** Typesafe ({@code typesafe}). */
    TYPESAFE(ModelProvider.TYPESAFE, "typesafe"),

    /** Voyage AI ({@code voyage_ai}). */
    VOYAGE_AI(ModelProvider.VOYAGE_AI, "voyage_ai"),

    /** IBM watsonx.ai ({@code ibm.watsonx.ai}). */
    WATSONX(ModelProvider.WATSONX, "ibm.watsonx.ai"),

    /** Unknown or unrecognized provider ({@code unknown}). */
    OTHER(ModelProvider.OTHER, "unknown");

    private final ModelProvider modelProvider;
    private final String value;

    /**
     * @param modelProvider the LangChain4j model provider
     * @param value the OpenTelemetry semantic convention provider name
     */
    private GenAIProviders(ModelProvider modelProvider, String value) {
        this.modelProvider = modelProvider;
        this.value = value;
    }

    /**
     * Returns the LangChain4j model provider mapped by this constant.
     *
     * @return the model provider
     */
    public ModelProvider getModelProvider() {
        return modelProvider;
    }

    /**
     * Returns the {@code GenAIProviders} constant corresponding to the given model provider, or {@link #OTHER} if none
     * matches.
     *
     * @param modelProvider the model provider to look up
     * @return the matching constant, or {@link #OTHER}
     */
    public static GenAIProviders of(final ModelProvider modelProvider) {
        for (GenAIProviders providers : values()) {
            if (providers.modelProvider == modelProvider) return providers;
        }

        return OTHER;
    }

    @Override
    public String toString() {
        return value;
    }
}
