package dev.langchain4j.cdi.integrationtests;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** JAX-RS endpoint for testing thinking capture via {@code @OnThinking} and {@code listenerNames}. */
@Path("/thinking-chat")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.APPLICATION_JSON)
public class ThinkingChatRestService {

    private final Object lock = new Object();

    @Inject
    ThinkingChatAiService thinkingChatAiService;

    @Inject
    ListenerChatAiService listenerChatAiService;

    @Inject
    ThinkingListener thinkingListener;

    public ThinkingChatRestService() {}

    /** Calls the {@code @OnThinking} service and returns the captured thinking text. */
    @POST
    @Path("/on-thinking")
    public String postOnThinking(String chatRequest) {
        return captureThinking(
                () -> ThinkingChatAiService.LAST_THINKING.set(null),
                msg -> thinkingChatAiService.chat(msg),
                () -> ThinkingChatAiService.LAST_THINKING.get(),
                chatRequest);
    }

    /** Calls the {@code listenerNames} service and returns the captured thinking text. */
    @POST
    @Path("/listener")
    public String postListener(String chatRequest) {
        return captureThinking(
                thinkingListener::reset,
                msg -> listenerChatAiService.chat(msg),
                thinkingListener::getLastThinking,
                chatRequest);
    }

    private String captureThinking(
            Runnable reset, Consumer<String> serviceCall, Supplier<String> getThinking, String chatRequest) {
        synchronized (lock) {
            reset.run();
            serviceCall.accept(chatRequest);
            String thinking = getThinking.get();
            return thinking != null ? thinking : "";
        }
    }
}
