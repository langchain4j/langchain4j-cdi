package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.protocol.McpRoot;
import java.util.List;

/** Provides access to the client's file system roots. */
public interface Roots {

    boolean isSupported();

    <T> T list();

    List<McpRoot> listAndAwait();
}
