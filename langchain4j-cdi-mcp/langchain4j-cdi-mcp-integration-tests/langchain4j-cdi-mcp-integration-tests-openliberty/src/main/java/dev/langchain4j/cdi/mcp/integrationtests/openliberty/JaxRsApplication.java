package dev.langchain4j.cdi.mcp.integrationtests.openliberty;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/")
public class JaxRsApplication extends Application {}
