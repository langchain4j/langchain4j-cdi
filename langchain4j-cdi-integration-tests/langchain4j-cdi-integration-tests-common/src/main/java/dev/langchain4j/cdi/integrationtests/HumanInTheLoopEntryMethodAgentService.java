package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.cdi.spi.RegisterHumanInTheLoopAgent;
import dev.langchain4j.service.V;

@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterHumanInTheLoopAgent(
        name = "hitl-entry-method-agent",
        description = "Entry-method HITL agent for integration testing",
        outputKey = "entryMethodResult")
public interface HumanInTheLoopEntryMethodAgentService {

    static String askUser(AgenticScope scope) {
        return "entry-method-response";
    }

    String process(@V("input") String input);
}
