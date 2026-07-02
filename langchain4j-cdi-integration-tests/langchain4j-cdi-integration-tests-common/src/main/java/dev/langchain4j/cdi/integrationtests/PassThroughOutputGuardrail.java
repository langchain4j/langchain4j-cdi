package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import jakarta.enterprise.context.ApplicationScoped;

/** Output guardrail that always passes through. */
@ApplicationScoped
public class PassThroughOutputGuardrail implements OutputGuardrail {

    /** Creates a new instance. */
    public PassThroughOutputGuardrail() {}

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        return success();
    }
}
