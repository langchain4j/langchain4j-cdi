package dev.langchain4j.cdi.example.booking;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** JAX-RS application configuration for the car booking service. */
@ApplicationPath("/")
public class CarBookingApplication extends Application {

    /** Creates a new car booking application. */
    public CarBookingApplication() {}
}
