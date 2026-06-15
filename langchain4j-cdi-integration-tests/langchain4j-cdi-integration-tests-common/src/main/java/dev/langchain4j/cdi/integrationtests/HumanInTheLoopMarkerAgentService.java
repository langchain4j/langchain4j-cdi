package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.cdi.spi.RegisterHumanInTheLoopAgent;

@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterHumanInTheLoopAgent(
        name = "hitl-marker-agent",
        description = "Marker interface HITL agent for integration testing",
        outputKey = "markerResult")
public interface HumanInTheLoopMarkerAgentService {

    static String askUser(AgenticScope scope) {
        return "marker-response";
    }
}
