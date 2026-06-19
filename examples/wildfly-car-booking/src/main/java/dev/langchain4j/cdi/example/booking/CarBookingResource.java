package dev.langchain4j.cdi.example.booking;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@ApplicationScoped
@Path("/car-booking")
public class CarBookingResource {
    private static final Logger LOGGER = Logger.getLogger(CarBookingResource.class.getName());

    @Inject
    private ChatAiService aiService;

    @Inject
    private FraudAiService fraudService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/chat")
    @Operation(
            summary = "Chat with an assistant.",
            description = "Ask any car booking related question.",
            operationId = "chatWithAssistant")
    @APIResponse(
            responseCode = "200",
            description = "Answer provided by assistant",
            content = @Content(mediaType = "text/plain"))
    public Response chatWithAssistant(
            @Parameter(
                            description = "The question to ask the assistant",
                            required = true,
                            example = "I want to book a car how can you help me?")
                    @QueryParam("question")
                    String question) {

        try {
            return Response.ok(aiService.chat(question)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while chatting with assistant for question: " + question, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("My failure reason is:\n\n" + e.getMessage())
                    .build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fraud")
    @Operation(
            summary = "Detect for a customer.",
            description = "Detect fraud for a customer given his name and surname.",
            operationId = "detectFraudForCustomer")
    @APIResponse(
            responseCode = "200",
            description = "Answer provided by assistant",
            content = @Content(mediaType = "application/json"))
    public Response detectFraudForCustomer(
            @Parameter(description = "Name of the customer to detect fraud for.", required = true, example = "Bond")
                    @QueryParam("name")
                    String name,
            @QueryParam("surname")
                    @Parameter(
                            description = "Surname of the customer to detect fraud for.",
                            required = true,
                            example = "James")
                    String surname) {
        try {
            return Response.ok(fraudService.detectFraudForCustomer(name, surname))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while detecting fraud for customer " + name + " " + surname, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("My failure reason is:\n\n" + e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }
}
