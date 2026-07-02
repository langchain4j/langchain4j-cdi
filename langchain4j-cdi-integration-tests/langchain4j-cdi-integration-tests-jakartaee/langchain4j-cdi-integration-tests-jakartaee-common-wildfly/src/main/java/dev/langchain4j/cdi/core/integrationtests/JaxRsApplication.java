package dev.langchain4j.cdi.core.integrationtests;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** JAX-RS application definition for integration testing. */
@ApplicationPath("/")
public class JaxRsApplication extends Application {

    /** Creates a new instance. */
    public JaxRsApplication() {}
}
