package dev.langchain4j.cdi.integrationtests;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Shared helper for thinking integration tests across runtimes. */
public final class ThinkingTestHelper {

    private ThinkingTestHelper() {}

    public static String postAndGetThinking(WebTarget baseTarget, String subPath) {
        Response response = baseTarget
                .path("/thinking-chat/" + subPath)
                .request(MediaType.TEXT_PLAIN)
                .post(Entity.entity("What is 2+2?", MediaType.APPLICATION_JSON));
        if (response.getStatus() != 200) {
            throw new AssertionError(
                    "Expected status 200 but got " + response.getStatus() + " from /thinking-chat/" + subPath);
        }
        return response.readEntity(String.class);
    }
}
