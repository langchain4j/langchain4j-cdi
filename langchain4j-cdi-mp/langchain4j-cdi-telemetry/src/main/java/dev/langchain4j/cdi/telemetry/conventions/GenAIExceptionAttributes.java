/** */
package dev.langchain4j.cdi.telemetry.conventions;

/**
 * Enum representing the exception attributes and event name for Generative AI client operations, as defined in the
 * OpenTelemetry Semantic Conventions for GenAI exceptions.
 *
 * @author Buhake Sindi
 * @since 12 October 2026
 * @see <a
 *     href="https://github.com/open-telemetry/semantic-conventions-genai/blob/main/docs/gen-ai/gen-ai-exceptions.md">GenAI
 *     Exceptions</a>
 */
public enum GenAIExceptionAttributes {

    /** Client operation exception event name ({@code gen_ai.client.operation.exception}). */
    CLIENT_OPERATION_EXCEPTION_EVENT("gen_ai.client.operation.exception"),

    /** Exception message ({@code exception.message}). */
    EXCEPTION_MESSAGE("exception.message"),

    /** Exception type ({@code exception.type}). */
    EXCEPTION_TYPE("exception.type"),

    /** Exception stacktrace ({@code exception.stacktrace}). */
    EXCEPTION_STACKTRACE("exception.stacktrace");

    private final String value;

    /** @param value the OpenTelemetry semantic convention attribute key */
    private GenAIExceptionAttributes(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
