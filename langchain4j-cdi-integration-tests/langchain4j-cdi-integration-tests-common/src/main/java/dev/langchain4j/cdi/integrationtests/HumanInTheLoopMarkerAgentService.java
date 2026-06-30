package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.cdi.spi.RegisterHumanInTheLoopAgent;

/** Human-in-the-loop agent using marker interface for integration testing. */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterHumanInTheLoopAgent(
        name = "hitl-marker-agent",
        description = "Marker interface HITL agent for integration testing",
        outputKey = "markerResult")
public interface HumanInTheLoopMarkerAgentService {

    /**
     * Simulates asking the user for input.
     *
     * @param scope the agentic scope
     * @return a fixed test response
     */
    static String askUser(AgenticScope scope) {
        return "marker-response";
    }
}
