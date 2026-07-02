package dev.langchain4j.cdi.core.integrationtests;

import dev.langchain4j.cdi.integrationtests.HumanInTheLoopEntryMethodAgentService;
import dev.langchain4j.cdi.integrationtests.HumanInTheLoopMarkerAgentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** JAX-RS endpoint for human-in-the-loop agent integration testing. */
@Path("/hitl-agent")
@Produces(MediaType.TEXT_PLAIN)
public class HumanInTheLoopRestService {

    @Inject
    HumanInTheLoopMarkerAgentService markerAgent;

    @Inject
    HumanInTheLoopEntryMethodAgentService entryMethodAgent;

    /** Creates a new instance. */
    public HumanInTheLoopRestService() {}

    /**
     * Returns info about the marker-based HITL agent.
     *
     * @return the agent string representation
     */
    @GET
    @Path("/marker")
    public String markerAgentInfo() {
        return markerAgent.toString();
    }

    /**
     * Returns info about the entry-method-based HITL agent.
     *
     * @return the agent string representation
     */
    @GET
    @Path("/entry-method")
    public String entryMethodAgentInfo() {
        return entryMethodAgent.toString();
    }
}
