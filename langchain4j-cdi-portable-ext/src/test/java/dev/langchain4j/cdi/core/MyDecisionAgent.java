/** */
package dev.langchain4j.cdi.core;

import dev.langchain4j.cdi.spi.RegisterDecisionAgent;
import dev.langchain4j.model.decision.response.YesNoAnswer;
import dev.langchain4j.service.V;
import dev.langchain4j.service.decision.Decide;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Buhake Sindi
 * @since 30 October 2026
 */
@RegisterDecisionAgent(
        scope = ApplicationScoped.class,
        description = "Checks whether a message is spam",
        outputKey = "spam")
public interface MyDecisionAgent {

    @Decide("Is this message spam?")
    YesNoAnswer isSpam(@V("message") String message);
}
