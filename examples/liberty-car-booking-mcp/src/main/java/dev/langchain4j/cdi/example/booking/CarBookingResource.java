package dev.langchain4j.cdi.example.booking;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** REST resource for car booking AI services. */
@ApplicationScoped
@Path("/car-booking")
public class CarBookingResource {

    @Inject
    private ChatAiService aiService;

    @Inject
    private FraudAiService fraudService;

    /** Creates a new car booking resource. */
    public CarBookingResource() {}

    /**
     * Chats with the car booking assistant.
     *
     * @param question the question to ask
     * @return the assistant's response
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/chat")
    public String chatWithAssistant(@QueryParam("question") String question) {

        String answer;
        try {
            answer = aiService.chat(question);
        } catch (Exception e) {
            e.printStackTrace();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(byteArrayOutputStream));
            answer = "My failure reason is:\n\n" + e.getMessage() + "\n\n"
                    + byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        }

        return answer;
    }

    /**
     * Detects fraud for a customer.
     *
     * @param name the customer name
     * @param surname the customer surname
     * @return the fraud detection response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fraud")
    public FraudResponse detectFraudForCustomer(
            @QueryParam("name") String name, @QueryParam("surname") String surname) {
        return fraudService.detectFraudForCustomer(name, surname);
    }
}
