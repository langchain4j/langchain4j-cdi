package dev.langchain4j.cdi.mcp.server.transport;

import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link McpSseChannel} backed by a Jakarta REST {@link SseEventSink}. Unlike a raw response output stream, the sink is
 * flushed by the Jakarta REST runtime after every event, so events reach the client immediately on every runtime, and
 * it stays usable after the resource method returns, so no request thread is held for the lifetime of the stream. A
 * closed sink, a send that throws or a send whose completion stage fails marks the channel closed.
 */
public class McpSseEventSinkChannel implements McpSseChannel {

    private static final Logger LOGGER = Logger.getLogger(McpSseEventSinkChannel.class.getName());
    private static final String EVENT_NAME = "message";

    private final SseEventSink sink;
    private final Sse sse;
    private final AtomicBoolean cancelledFlag;
    private volatile boolean open = true;

    /**
     * Creates a channel sending events to the given sink.
     *
     * @param sink the event sink of the current response
     * @param sse the Jakarta REST SSE entry point used to build events
     * @param cancelledFlag flag set to {@code true} when the client is found gone, or {@code null}
     */
    public McpSseEventSinkChannel(SseEventSink sink, Sse sse, AtomicBoolean cancelledFlag) {
        this.sink = sink;
        this.sse = sse;
        this.cancelledFlag = cancelledFlag;
    }

    @Override
    public void sendData(String json) {
        send(sse.newEventBuilder().name(EVENT_NAME).data(json).build());
    }

    @Override
    public void sendComment(String comment) {
        send(sse.newEventBuilder().comment(comment).build());
    }

    @Override
    public boolean isOpen() {
        if (open && sink.isClosed()) {
            broken(null);
        }
        return open;
    }

    @Override
    public void close() {
        open = false;
        try {
            sink.close();
        } catch (RuntimeException e) {
            LOGGER.log(Level.FINE, "MCP: failed to close SSE event sink", e);
        }
    }

    private synchronized void send(OutboundSseEvent event) {
        if (!isOpen()) {
            return;
        }
        try {
            sink.send(event).whenComplete((ignored, failure) -> {
                if (failure != null) {
                    broken(failure);
                }
            });
        } catch (RuntimeException e) {
            broken(e);
        }
    }

    private void broken(Throwable failure) {
        if (open) {
            LOGGER.log(Level.FINE, "MCP: SSE event sink closed by client", failure);
        }
        open = false;
        try {
            sink.close();
        } catch (RuntimeException e) {
            LOGGER.log(Level.FINE, "MCP: failed to close SSE event sink after send failure", e);
        }
        if (cancelledFlag != null) {
            cancelledFlag.set(true);
        }
    }
}
