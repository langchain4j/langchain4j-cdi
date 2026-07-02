package dev.langchain4j.cdi.mcp.integrationtests;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** JAX-RS application definition for MCP integration testing. */
@ApplicationPath("/")
public class JaxRsApplication extends Application {

    /** Creates a new instance. */
    public JaxRsApplication() {}
}
