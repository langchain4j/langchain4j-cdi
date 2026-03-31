package dev.langchain4j.cdi.mcp.server.api;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mcp_java.server.Cancellation;

/** Implementation of {@link Cancellation} backed by an {@link AtomicBoolean} flag from the request context. */
public class CdiCancellation implements Cancellation {

    private final AtomicBoolean cancelledFlag;

    public CdiCancellation(AtomicBoolean cancelledFlag) {
        this.cancelledFlag = cancelledFlag;
    }

    @Override
    public Result check() {
        return new Result(cancelledFlag.get(), Optional.empty());
    }
}
