package dev.langchain4j.cdi.integrationtests;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** JAX-RS endpoint for agent chat integration testing. */
@Path("/agent-chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentChatRestService {

    @Inject
    AgentChatService agentChatService;

    /** Creates a new instance. */
    public AgentChatRestService() {}

    /**
     * Forwards a chat request to the agent chat service.
     *
     * @param chatRequest the user message
     * @return the agent response
     */
    @POST
    public String postChat(String chatRequest) {
        return agentChatService.chat(chatRequest);
    }
}
