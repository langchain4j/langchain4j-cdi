package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.cdi.spi.RegisterHumanInTheLoopAgent;
import dev.langchain4j.service.V;

/** Human-in-the-loop agent using an entry method for integration testing. */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterHumanInTheLoopAgent(
        name = "hitl-entry-method-agent",
        description = "Entry-method HITL agent for integration testing",
        outputKey = "entryMethodResult")
public interface HumanInTheLoopEntryMethodAgentService {

    /**
     * Simulates asking the user for input.
     *
     * @param scope the agentic scope
     * @return a fixed test response
     */
    static String askUser(AgenticScope scope) {
        return "entry-method-response";
    }

    /**
     * Processes the given input.
     *
     * @param input the input to process
     * @return the processing result
     */
    String process(@V("input") String input);
}
