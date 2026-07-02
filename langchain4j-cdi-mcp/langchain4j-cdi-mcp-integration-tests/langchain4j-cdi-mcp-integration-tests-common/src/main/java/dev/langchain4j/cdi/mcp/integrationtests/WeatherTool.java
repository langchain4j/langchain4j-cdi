package dev.langchain4j.cdi.mcp.integrationtests;

import jakarta.enterprise.context.ApplicationScoped;
import org.mcp_java.annotations.tools.Tool;
import org.mcp_java.annotations.tools.ToolArg;

/** MCP tool that returns weather information for a city. */
@ApplicationScoped
public class WeatherTool {

    /** Creates a new instance. */
    public WeatherTool() {}

    /**
     * Returns a fixed weather string for the given city.
     *
     * @param city the city name
     * @param unit the temperature unit (celsius or fahrenheit)
     * @return the weather description
     */
    @Tool(description = "Get the current weather for a given city")
    public String getWeather(
            @ToolArg(description = "The city name") String city,
            @ToolArg(description = "Unit: celsius or fahrenheit") String unit) {
        return "Sunny, 22C in " + city;
    }
}
