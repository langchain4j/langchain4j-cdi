package dev.langchain4j.cdi.core.integrationtests;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** JAX-RS endpoint for expression-resolved chat integration testing. */
@Path("/expression-chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExpressionChatRestService {

    @Inject
    ExpressionChatAiService expressionChatAiService;

    /** Creates a new instance. */
    public ExpressionChatRestService() {}

    /**
     * Forwards a chat request to the expression-resolved AI service.
     *
     * @param chatRequest the user message
     * @return the AI response
     */
    @POST
    public String postChat(String chatRequest) {
        return expressionChatAiService.chat(chatRequest);
    }
}
