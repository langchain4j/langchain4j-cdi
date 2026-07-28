---
title: Contributing
description: How to build, test, and contribute to the LangChain4j CDI project.
layout: page
---

# Contributing

## Build Requirements

- Java 21+ (runtime target is Java 17)
- Maven 3.8+

## Building

Build and compile (skip tests):

```bash
mvn clean install -DskipTests -pl '!examples/payara-car-booking'
```

Full build with tests:

```bash
mvn clean verify -pl '!examples/payara-car-booking'
```

Always exclude `examples/payara-car-booking` due to network connectivity issues.

## Testing

Run unit tests only:

```bash
mvn test -pl '!examples/payara-car-booking,!langchain4j-cdi-integration-tests'
```

Run all tests including integration:

```bash
mvn test -pl '!examples/payara-car-booking'
```

Run Quarkus integration tests:

```bash
mvn test -pl langchain4j-cdi-integration-tests/langchain4j-cdi-integration-tests-quarkus
```

Run Jakarta EE integration tests (longer due to server startup):

```bash
mvn test -pl langchain4j-cdi-integration-tests/langchain4j-cdi-integration-tests-jakartaee
```

## Code Formatting

The project uses Palantir Java Format via Spotless.

Check formatting:

```bash
mvn spotless:check -pl '!examples/payara-car-booking'
```

Auto-fix formatting:

```bash
mvn spotless:apply -pl '!examples/payara-car-booking'
```

## Project Structure

| Module | Description |
|--------|-------------|
| `langchain4j-cdi-core` | Foundation -- `@RegisterAIService`, agent annotations, SPI |
| `langchain4j-cdi-portable-ext` | CDI portable extension (WildFly, GlassFish, Liberty) |
| `langchain4j-cdi-build-compatible-ext` | Build-time CDI extension (Quarkus, Helidon) |
| `langchain4j-cdi-el` | Jakarta EL expression resolver |
| `langchain4j-cdi-a2a` | A2A (Agent-to-Agent) topology wiring |
| `langchain4j-cdi-mp` | MicroProfile integration (config, fault tolerance, telemetry) |
| `langchain4j-cdi-mcp` | MCP server module tree |

## Guidelines

- **Backward Compatibility** -- Required. Use `@Deprecated` instead of removing fields/methods.
- **No Lombok** -- Avoid in new code; remove from old code when possible.
- **Minimize Dependencies** -- Check with `mvn dependency:analyze`.
- **Test with Quarkus** -- At minimum, validate changes with the Quarkus example application.

## Workflow

1. Build and test current state
2. Make changes in the appropriate module
3. Add unit tests in the same module
4. Add integration tests if the change affects runtime behavior
5. Format code: `mvn spotless:apply`
6. Validate: `mvn spotless:check verify -pl '!examples/payara-car-booking'`
