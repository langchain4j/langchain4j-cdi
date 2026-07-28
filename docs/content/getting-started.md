---
title: Getting Started
description: Get started with LangChain4j CDI -- overview, setup, and first AI service.
layout: page
---

# Getting Started

LangChain4j CDI is a CDI (Contexts and Dependency Injection) extension that integrates the [LangChain4j](https://docs.langchain4j.dev/) AI framework with Jakarta EE and Eclipse MicroProfile applications. It enables developers to inject AI services into CDI-managed beans with enterprise features like fault tolerance, telemetry, and external configuration.

## What You Can Build

- **AI Services** -- Declare AI service interfaces and inject them anywhere in your CDI application.
- **Agent Orchestration** -- Wire multi-agent topologies (sequence, loop, parallel, supervisor, etc.) using CDI annotations.
- **MCP Servers** -- Expose CDI beans as Model Context Protocol servers with tools, prompts, and resources.
- **Enterprise AI** -- Add fault tolerance, telemetry, and external configuration to your AI services.

## Prerequisites

- Java 17+ (Java 21+ recommended for building)
- Maven 3.8+
- A CDI container (Jakarta EE server or CDI-capable framework)

## Quick Start

### 1. Add the dependency

Choose the extension that matches your runtime:

**For Quarkus / Helidon (build-time extension):**

```xml
<dependency>
    <groupId>dev.langchain4j.cdi</groupId>
    <artifactId>langchain4j-cdi-build-compatible-ext</artifactId>
    <version>$\{langchain4j-cdi.version}</version>
</dependency>
```

**For WildFly / GlassFish / Liberty / Payara (portable extension):**

```xml
<dependency>
    <groupId>dev.langchain4j.cdi</groupId>
    <artifactId>langchain4j-cdi-portable-ext</artifactId>
    <version>$\{langchain4j-cdi.version}</version>
</dependency>
```

### 2. Define an AI Service

```java
@RegisterAIService(chatModelName = "#default")
public interface ChatBot {
    String chat(String userMessage);
}
```

### 3. Inject and Use

```java
@ApplicationScoped
public class MyBean {

    @Inject
    ChatBot chatBot;

    public String askQuestion(String question) {
        return chatBot.chat(question);
    }
}
```

## Guides

| Guide | Description |
|-------|-------------|
| [AI Services]({site.url('ai-services')}) | Declare and configure AI service interfaces |
| [Agents]({site.url('agents')}) | Wire multi-agent topologies via CDI annotations |
| [MCP Server]({site.url('mcp-server')}) | Turn CDI beans into MCP servers |
| [Expression Language]({site.url('expression-language')}) | Dynamic attribute resolution via Jakarta EL and MicroProfile Config |
| [Configuration]({site.url('configuration')}) | External configuration via MicroProfile Config |
| [Fault Tolerance]({site.url('fault-tolerance')}) | Retry, timeout, circuit breaker, fallback |
| [Telemetry]({site.url('telemetry')}) | OpenTelemetry observability for AI calls |
| [Examples]({site.url('examples')}) | Sample applications for various runtimes |
