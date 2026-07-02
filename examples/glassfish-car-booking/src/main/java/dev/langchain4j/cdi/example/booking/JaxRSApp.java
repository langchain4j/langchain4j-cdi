package dev.langchain4j.cdi.example.booking;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** JAX-RS application configuration. */
@ApplicationPath("/api")
public class JaxRSApp extends Application {

    /** Creates a new JAX-RS application. */
    public JaxRSApp() {}
}
