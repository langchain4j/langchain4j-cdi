package dev.langchain4j.cdi.mcp.server.api;

import org.mcpjava.server.progress.ProgressToken;

/** Implementation of {@link ProgressToken} backed by a raw token value. */
public class CdiProgressToken implements ProgressToken {

    private final Object rawToken;

    CdiProgressToken(Object rawToken) {
        if (rawToken == null) {
            throw new IllegalArgumentException("rawToken must not be null");
        }
        this.rawToken = rawToken;
    }

    public static ProgressToken of(Object rawToken) {
        return new CdiProgressToken(rawToken);
    }

    @Override
    public Type type() {
        return rawToken instanceof Number ? Type.INTEGER : Type.STRING;
    }

    @Override
    public Number asInteger() {
        return rawToken instanceof Number n ? n : null;
    }

    @Override
    public String asString() {
        return rawToken.toString();
    }
}
