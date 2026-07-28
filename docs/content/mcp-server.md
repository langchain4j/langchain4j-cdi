---
title: MCP Server
description: Turn CDI beans into Model Context Protocol servers exposing tools, prompts, and resources.
layout: page
---

# MCP Server

The `langchain4j-cdi-mcp` module tree turns CDI beans into a **Model Context Protocol (MCP) server**. It exposes annotated bean methods as MCP **tools**, **prompts**, and **resources** over JSON-RPC 2.0 / Streamable HTTP at the `/mcp` endpoint.

Built on the [MCP Java](https://github.com/mcp-java) project (`java-mcp-annotations`).

## Quick Setup

Add the MCP server dependency:

```xml
<dependency>
    <groupId>dev.langchain4j.cdi.mcp</groupId>
    <artifactId>langchain4j-cdi-mcp-server</artifactId>
    <version>$\{langchain4j-cdi-mcp.version}</version>
</dependency>
```

And the appropriate CDI extension for your runtime:

**Quarkus / Helidon:**
```xml
<dependency>
    <groupId>dev.langchain4j.cdi.mcp</groupId>
    <artifactId>langchain4j-cdi-mcp-build-compatible-ext</artifactId>
    <version>$\{langchain4j-cdi-mcp.version}</version>
</dependency>
```

**WildFly / GlassFish / Liberty:**
```xml
<dependency>
    <groupId>dev.langchain4j.cdi.mcp</groupId>
    <artifactId>langchain4j-cdi-mcp-portable-ext</artifactId>
    <version>$\{langchain4j-cdi-mcp.version}</version>
</dependency>
```

## Defining Tools

```java
@ApplicationScoped
public class WeatherService {

    @Tool("Get current weather for a city")
    public String getWeather(
            @ToolArg(description = "City name") String city) {
        return "Sunny, 25C in " + city;
    }
}
```

## Defining Prompts

```java
@ApplicationScoped
public class PromptLibrary {

    @Prompt("Generate a summary prompt")
    public String summarize(
            @PromptArg(description = "Text to summarize") String text) {
        return "Please summarize the following text:\n\n" + text;
    }
}
```

## Defining Resources

```java
@ApplicationScoped
public class DataResources {

    @Resource(uri = "data://config", description = "Application configuration")
    public String getConfig() {
        return loadConfiguration();
    }
}
```

## MCP Capabilities

The MCP server supports these capabilities via framework types injected at invocation time:

| Type | Description |
|------|-------------|
| `McpLog` | Structured logging within tool execution |
| `Progress` | Report progress to the client |
| `Cancellation` | Check if the client cancelled the request |
| `McpConnection` | Access to the MCP session |
| `Roots` | Access client-provided root URIs |
| `Sampling` | Request LLM sampling from the client |
| `Elicitation` | Request user input from the client |

These parameters are excluded from the generated JSON Schema.

## Architecture

- **`McpEndpoint`** -- JAX-RS `/mcp` resource handling JSON-RPC requests
- **`McpSessionManager`** -- Session lifecycle with 30-minute default expiry
- **`McpNotificationBroadcaster`** -- SSE notifications to connected clients
- **`JsonSchemaGenerator`** -- Automatic JSON Schema generation from Java method signatures
