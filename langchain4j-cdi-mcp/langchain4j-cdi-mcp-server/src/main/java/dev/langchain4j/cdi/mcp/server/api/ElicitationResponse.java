package dev.langchain4j.cdi.mcp.server.api;

import java.util.List;
import java.util.Map;

/** Holds the result of a user elicitation request. */
public interface ElicitationResponse {

    /** Whether the user accepted or declined the elicitation. */
    enum Action {
        ACCEPT,
        DECLINE
    }

    /** Typed accessors for the values returned by the user. */
    interface Content {

        Boolean getBoolean(String key);

        String getString(String key);

        List<String> getStrings(String key);

        Integer getInteger(String key);

        Number getNumber(String key);

        Map<String, Object> asMap();
    }

    Action action();

    Content content();
}
