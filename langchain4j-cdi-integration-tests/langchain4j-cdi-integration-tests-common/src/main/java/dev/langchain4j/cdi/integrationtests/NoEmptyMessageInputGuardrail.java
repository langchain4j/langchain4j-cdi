package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import jakarta.enterprise.context.ApplicationScoped;

/** Input guardrail that rejects empty or blank messages. */
@ApplicationScoped
public class NoEmptyMessageInputGuardrail implements InputGuardrail {

    /** Creates a new instance. */
    public NoEmptyMessageInputGuardrail() {}

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String text = userMessage.singleText();
        if (text == null || text.isBlank()) {
            return fatal("Message must not be empty");
        }
        return success();
    }
}
