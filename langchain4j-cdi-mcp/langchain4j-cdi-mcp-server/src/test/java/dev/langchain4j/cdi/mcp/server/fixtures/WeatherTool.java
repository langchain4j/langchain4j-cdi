package dev.langchain4j.cdi.mcp.server.fixtures;

import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;

public class WeatherTool {

    @Tool(description = "Get the current weather for a given city")
    public String getWeather(
            @ToolArg(description = "The city name") String city,
            @ToolArg(description = "Unit: celsius or fahrenheit") String unit) {
        return "Sunny, 22C in " + city;
    }
}
