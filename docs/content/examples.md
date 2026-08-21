---
title: Examples
description: Sample applications demonstrating LangChain4j CDI with various Jakarta EE runtimes.
layout: page
---

# Examples

## Articles and Presentations

| Resource | Description |
|----------|-------------|
| [WildFly AI Chat Bot Guide](https://www.wildfly.org/guides/langchain-ai-chat-bot/) | Step-by-step guide for building an AI chat bot with WildFly and LangChain4j CDI |
| [LangChain4j CDI Presentation (Fujitsu)](https://speakerdeck.com/kazumura/langchain4jtoha-wei-wei-ulangchain4j-cdi) | Presentation on LangChain4j and LangChain4j CDI by Fujitsu |
| [Fujitsu LangChain4j CDI Samples](https://github.com/fujitsu/app_blog/tree/master/lc4j-cdi-samples) | Sample applications demonstrating LangChain4j CDI usage by Fujitsu |

## Workshops

Hands-on workshops to learn LangChain4j CDI step by step:

| Workshop | Description |
|----------|-------------|
| [Liberty LangChain4j Workshop](https://github.com/OpenLiberty/liberty-workshop-langchain4j) | Open Liberty workshop for building AI-powered applications with LangChain4j |
| [LangChain4j CDI Lab 2026](https://github.com/ehsavoie/langchain4j-cdi-lab-2026) | Practical lab exercises covering LangChain4j CDI integration |
| [LangChain4j CDI Lab 2026 (JCON Slovenia)](https://github.com/TheEliteGentleman/langchain4j-cdi-lab-jcon-slovenia-2026) | Practical lab exercises covering LangChain4j CDI integration (for JCON Slovenia 2026) |

## Sample Applications

All examples demonstrate a car booking application with chat, fraud detection, and function calling.

### Recommended: Quarkus

The fastest way to get started:

```bash
cd examples/quarkus-car-booking
mvn quarkus:dev
```

Startup time is around 10 seconds with live reload support.

### Available Examples

| Example | Runtime | Extension Type |
|---------|---------|---------------|
| `quarkus-car-booking` | Quarkus | Build-compatible |
| `helidon-car-booking` | Helidon | Build-compatible |
| `helidon-car-booking-portable-ext` | Helidon | Portable |
| `wildfly-car-booking` | WildFly | Portable |
| `glassfish-car-booking` | GlassFish | Portable |
| `liberty-car-booking` | Open Liberty | Portable |
| `liberty-car-booking-mcp` | Open Liberty | Portable + MCP |
| `car-booking-mcp` | - | MCP server demo |

### Running an Example

1. Install all core modules first:

```bash
mvn clean install -DskipTests -pl '!examples/payara-car-booking'
```

2. Navigate to the example directory and run:

```bash
cd examples/quarkus-car-booking
mvn quarkus:dev
```

3. Without an LLM provider (Ollama), endpoints return connection errors (expected behavior).

4. With Ollama, use the provided setup scripts in the examples directory.

### MCP Server Example

The `car-booking-mcp` example demonstrates how to expose car booking operations as MCP tools:

```bash
cd examples/car-booking-mcp
mvn quarkus:dev
```

The MCP server is available at `http://localhost:8080/mcp`.

### Liberty MCP Example

The `liberty-car-booking-mcp` example demonstrates MCP server integration with Open Liberty:

```bash
cd examples/liberty-car-booking-mcp
mvn liberty:dev
```
