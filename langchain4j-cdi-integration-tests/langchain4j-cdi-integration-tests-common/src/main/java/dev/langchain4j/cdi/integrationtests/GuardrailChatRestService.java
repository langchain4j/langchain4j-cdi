package dev.langchain4j.cdi.integrationtests;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** JAX-RS endpoint for guarded chat integration testing. */
@Path("/guarded-chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GuardrailChatRestService {

    @Inject
    GuardrailChatAiService guardrailChatAiService;

    /** Creates a new instance. */
    public GuardrailChatRestService() {}

    /**
     * Forwards a chat request to the guarded AI service.
     *
     * @param chatRequest the user message
     * @return the response, or 400 if a guardrail rejects the message
     */
    @POST
    public Response postChat(String chatRequest) {
        try {
            String result = guardrailChatAiService.chat(chatRequest);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }
}
