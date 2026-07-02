package dev.langchain4j.cdi.integrationtests;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** JAX-RS endpoint for chat integration testing. */
@Path("/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatRestService {

    @Inject
    ChatAiService chatAiService;

    /** Creates a new instance. */
    public ChatRestService() {}

    /**
     * Forwards a chat request to the AI service.
     *
     * @param chatRequest the user message
     * @return the AI response
     */
    @POST
    public String postChat(String chatRequest) {
        return chatAiService.chat(chatRequest);
    }
}
