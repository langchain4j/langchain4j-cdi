<p align="center">
  <img src="langchain4j-cdi-logo.png" alt="LangChain4j CDI Logo" width="200"/>
</p>

<p align="center">
  <a href="http://www.apache.org/licenses/LICENSE-2.0"><img src="https://img.shields.io/github/license/smallrye/smallrye-llm.svg" alt="License"/></a>
  <a href="https://central.sonatype.com/search?q=dev.langchain4j.cdi%3Alangchain4j-cdi-parent"><img src="https://img.shields.io/maven-central/v/dev.langchain4j.cdi/langchain4j-cdi-parent?color=green" alt="Maven"/></a>
  <a href="https://github.com/langchain4j/langchain4j/actions/workflows/main.yaml"><img src="https://img.shields.io/github/actions/workflow/status/langchain4j/langchain4j-cdi/main.yaml?branch=main&style=for-the-badge&label=CI%20BUILD&logo=github" alt="Build Status"/></a>
  <a href="https://discord.gg/JzTFvyjG6R"><img src="https://img.shields.io/discord/1156626270772269217?logoColor=violet" alt="Discord"/></a>
</p>

# LangChain4j CDI Integration

Enterprise CDI extension for [LangChain4j](https://docs.langchain4j.dev/) — inject AI services directly into your Jakarta EE and MicroProfile applications.

## Documentation

Full documentation is available at **[langchain4j.github.io/langchain4j-cdi](https://langchain4j.github.io/langchain4j-cdi/)**.

## Features

- **AI Service Injection** — declare AI services as CDI beans using `@RegisterAIService`
- **Agentic Topologies** — 11 per-topology annotations (`@RegisterSimpleAgent`, `@RegisterSequenceAgent`, `@RegisterLoopAgent`, etc.) for multi-agent workflows
- **MCP Server** — expose CDI beans as a Model Context Protocol server
- **Configuration via Properties** — configure LLM components through MicroProfile Config or a custom SPI
- **Fault Tolerance** — resilience with `@Retry`, `@Timeout`, `@CircuitBreaker`, `@Fallback`
- **Telemetry** — OpenTelemetry-based observability for AI operations
- **Expression Language** — resolve `${...}` (MicroProfile Config) and `#{...}` (Jakarta EL) expressions in annotations
- **Guardrails** — input and output validation for AI service interactions

## Supported Runtimes

| Runtime | Extension Type |
|---------|---------------|
| Quarkus | Build-compatible |
| Helidon | Both |
| WildFly | Portable |
| Payara | Portable |
| GlassFish | Portable |
| Liberty | Portable |

## Quick Start

**1. Add the dependency:**

```xml
<!-- For portable extension (WildFly, Payara, GlassFish, Liberty) -->
<dependency>
    <groupId>dev.langchain4j.cdi</groupId>
    <artifactId>langchain4j-cdi-portable-ext</artifactId>
    <version>${langchain4j-cdi.version}</version>
</dependency>

<!-- For build-compatible extension (Quarkus, Helidon) -->
<dependency>
    <groupId>dev.langchain4j.cdi</groupId>
    <artifactId>langchain4j-cdi-build-compatible-ext</artifactId>
    <version>${langchain4j-cdi.version}</version>
</dependency>
```

**2. Define an AI service:**

```java
@RegisterAIService
public interface AssistantService {

    @SystemMessage("You are a helpful assistant.")
    String chat(String userMessage);
}
```

**3. Inject and use:**

```java
@Path("/assistant")
public class AssistantResource {

    @Inject
    AssistantService assistant;

    @GET
    @Path("/chat")
    public String chat(@QueryParam("message") String message) {
        return assistant.chat(message);
    }
}
```

See the [documentation](https://langchain4j.github.io/langchain4j-cdi/) for configuration, agents, tools, RAG, guardrails, and more.

## Examples

Example applications are in the [`examples/`](examples/) directory, based on a car rental booking scenario. The [Quarkus example](examples/quarkus-car-booking) is recommended for the fastest development experience.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

## License

Apache License 2.0 — see [LICENSE](LICENSE) file.
