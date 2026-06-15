package dev.langchain4j.cdi.core.integrationtests;

import dev.langchain4j.cdi.integrationtests.HumanInTheLoopEntryMethodAgentService;
import dev.langchain4j.cdi.integrationtests.HumanInTheLoopMarkerAgentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hitl-agent")
@Produces(MediaType.TEXT_PLAIN)
public class HumanInTheLoopRestService {

    @Inject
    HumanInTheLoopMarkerAgentService markerAgent;

    @Inject
    HumanInTheLoopEntryMethodAgentService entryMethodAgent;

    @GET
    @Path("/marker")
    public String markerAgentInfo() {
        return markerAgent.toString();
    }

    @GET
    @Path("/entry-method")
    public String entryMethodAgentInfo() {
        return entryMethodAgent.toString();
    }
}
