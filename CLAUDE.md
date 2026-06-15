# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LangChain4j CDI is a CDI (Contexts and Dependency Injection) extension that integrates the LangChain4j AI framework with Jakarta EE and Eclipse MicroProfile applications. It enables developers to inject AI services into CDI-managed beans with enterprise features like fault tolerance, telemetry, and external configuration.

## Build System

**CRITICAL: Use `mvn` command only** - Never use `./mvnw` or `mvnw` wrappers, as per user's global preferences.

**Java Version Requirements:**
- Runtime target: Java 17
- Build requirement: Java 21+ (some integration tests require Java 21+)

**Payara Exclusion:** Always exclude the Payara example module due to network connectivity issues:
```bash
-pl '!examples/payara-car-booking'
```

### Essential Build Commands

Build and compile (skip tests):
```bash
mvn clean install -DskipTests -pl '!examples/payara-car-booking'
```

Full build with tests:
```bash
mvn clean verify -pl '!examples/payara-car-booking'
```

Run unit tests only (exclude integration tests):
```bash
mvn test -pl '!examples/payara-car-booking,!langchain4j-cdi-integration-tests'
```

Run all tests including integration:
```bash
mvn test -pl '!examples/payara-car-booking'
```

Check and apply code formatting:
```bash
mvn spotless:check -pl '!examples/payara-car-booking'
mvn spotless:apply -pl '!examples/payara-car-booking'
```

## Module Architecture

### Core Modules

**langchain4j-cdi-core**: Foundation of the CDI integration
- `@RegisterAIService` stereotype annotation for declaring AI service interfaces
- `CommonAIServiceCreator` utility for building AI service proxies from CDI beans
- 11 per-topology agent stereotype annotations (`@RegisterSimpleAgent`, `@RegisterSequenceAgent`, `@RegisterLoopAgent`, `@RegisterParallelAgent`, `@RegisterParallelMapperAgent`, `@RegisterConditionalAgent`, `@RegisterSupervisorAgent`, `@RegisterPlannerAgent`, `@RegisterA2AAgent`, `@RegisterMcpClientAgent`, `@RegisterHumanInTheLoopAgent`)
- `CommonAgentCreator` utility for building agent proxies across all 11 topologies
- `AgentTopologyType` enum defining available orchestration patterns
- `A2AAgentBuilder` SPI for pluggable A2A topology implementation (loaded via `ServiceLoader`)
- `LLMConfig` and `LLMConfigProvider` configuration API and SPI
- `CommonLLMPluginCreator` for creating LangChain4j components via reflection
- `ExpressionResolver` SPI for pluggable annotation attribute expression resolution

**langchain4j-cdi-portable-ext**: Portable CDI extension for runtime service registration
- Works with traditional Jakarta EE servers (WildFly, GlassFish, Liberty, Payara)
- `LangChain4JAIServicePortableExtension` processes `@RegisterAIService` annotations
- `LangChain4JPluginsPortableExtension` handles plugin bean registration

**langchain4j-cdi-build-compatible-ext**: Build-time CDI extension
- For ahead-of-time compilation frameworks (Quarkus, Helidon)
- Static analysis and bean registration at build time

**langchain4j-cdi-el**: Jakarta EL expression resolver
- Resolves `#{...}` expressions in annotation string attributes at runtime
- Uses `jakarta.el.ELProcessor`; wires CDI bean resolution when CDI is active
- Discovered automatically via `ServiceLoader` when on the classpath
- Requires a Jakarta EL implementation at runtime (e.g. `org.glassfish.expressly:expressly`)

**langchain4j-cdi-a2a**: A2A (Agent-to-Agent) topology CDI wiring
- Implements the `A2AAgentBuilder` SPI from `langchain4j-cdi-core`
- Loaded via `ServiceLoader` when A2A topology is used — optional module
- Wires `a2aServerUrl`, `outputKey`, `async`, and `agentListenerName` for A2A agents
- Requires `langchain4j-agentic-a2a` on the classpath for the A2A protocol implementation

### MicroProfile Integration Modules (langchain4j-cdi-mp)

**langchain4j-cdi-config**: MicroProfile Config integration
- External configuration via `microprofile-config.properties`
- Property-based configuration for chat models, embedding models, memory, etc.
- Pattern: `langchain4j.<component>.<name>.<property>`

**langchain4j-cdi-fault-tolerance**: MicroProfile Fault Tolerance integration
- `@Retry`, `@Timeout`, `@CircuitBreaker`, `@Fallback` support on AI service methods
- Resilient AI service calls with automatic retries and fallback mechanisms

**langchain4j-cdi-telemetry**: MicroProfile Telemetry integration
- OpenTelemetry-based observability for AI service calls
- Automatic metrics for request/response times, success/failure rates, token usage

## Key Extension Points

### Registering AI Services

AI services are declared using the `@RegisterAIService` annotation:

```java
@RegisterAIService(
    chatModelName = "#default",           // or specific bean name
    toolProviderName = "myTools",          // or use tools = { MyTool.class }
    chatMemoryName = "chat-memory",
    contentRetrieverName = "retriever"     // mutually exclusive with retrievalAugmentorName
)
public interface ChatAiService {
    String chat(String userMessage);
}
```

**Name Resolution:**
- `"#default"` = use the default CDI bean of that type
- Empty/blank = ignore this dependency
- Specific name = use the named bean
- `"${property.key}"` = resolved via MicroProfile Config (requires `langchain4j-cdi-config`)
- `"#{el.expression}"` = evaluated as Jakarta EL (requires `langchain4j-cdi-el`)

Resolution is performed by the `ExpressionResolver` SPI (`dev.langchain4j.cdi.spi.ExpressionResolver`) in `CdiLookupHelper.resolveExpression()`. Implementations are discovered via `ServiceLoader` and applied as a pipeline.

**Component Priority:**
- `RetrievalAugmentor` takes precedence over `ContentRetriever`
- `ToolProvider` is preferred over the `tools` array

### Registering Agents

Agents are declared using one of 11 per-topology stereotype annotations. Each annotation shares a common set of attributes and may include topology-specific and hook attributes.

**Common attributes (all 11 topologies):**

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `scope` | `Class<? extends Annotation>` | `ApplicationScoped.class` | CDI scope |
| `name` | `String` | `""` | Agent name |
| `description` | `String` | `""` | Agent description |
| `outputKey` | `String` | `""` | Key for storing output in `AgenticScope` |
| `typedOutputKey` | `Class<? extends TypedKey<?>>` | `Agent.NoTypedKey.class` | Type-safe output key (takes precedence over `outputKey`) |
| `optional` | `boolean` | `false` | Skip silently when arguments are missing |
| `summarizedContext` | `String[]` | `{}` | Agent names whose context to summarize and inject |
| `agentListenerName` | `String` | `""` | CDI bean name of an `AgentListener` |

Note: `async` is available on Simple, A2A, MCP, and HITL topologies only.

**Hook/callback attributes and topology availability:**

| Attribute | Bean type | Topologies |
|-----------|-----------|------------|
| `errorHandlerName` | `Function<ErrorContext, ErrorRecoveryResult>` | Sequence, Loop, Parallel, ParallelMapper, Conditional, Planner, Supervisor |
| `outputProviderName` | `Function<AgenticScope, Object>` | Sequence, Loop, Parallel, ParallelMapper, Conditional, Planner, Supervisor |
| `beforeCallName` | `Consumer<AgenticScope>` | Sequence, Loop, Parallel, ParallelMapper, Conditional, Planner |

These hooks resolve CDI beans by name. Due to Java type erasure on generics, beans are resolved as `Object` and cast — the bean must implement the correct functional interface. A warning is logged if the resolved bean has the wrong type.

Topologies missing an attribute lack `output()` or `beforeCall()` on their framework builder (`AgentBuilder`, `A2AAgentBuilder`, `McpService`, `HumanInTheLoop`, `SupervisorAgentService`). TODO comments in `CommonAgentCreator` track these gaps for future framework releases.

**Example — sequence agent with error handler and pre-execution hook:**

```java
@RegisterSequenceAgent(
    name = "pipeline",
    subAgentNames = {"step1", "step2", "step3"},
    errorHandlerName = "pipelineErrorHandler",
    beforeCallName = "pipelineSetup",
    outputProviderName = "pipelineOutput"
)
public interface PipelineAgent {}
```

**Agent proxy creation:**

`CommonAgentCreator` uses two proxy creation mechanisms:
- **`cdiAgentInstanceFactory`**: For `@Agent`-annotated simple agents — passed as the 3rd argument to `AgentConfigurator`, delegates to `AgentBuilder.interfacesToImplement()` to include all framework-required interfaces
- **`createAgentProxy`**: For non-`@Agent` simple agents and HITL — creates JDK proxies with `[userInterface, InternalAgent, AgenticScopeOwner, AgenticScopeAccess]` plus any extra interfaces

Both mechanisms ensure all agent proxies implement `AgenticScopeAccess` (added in the 1.16.2 upgrade).

### Configuration-Based Plugin Creation

Components can be created from configuration properties:

```properties
dev.langchain4j.cdi.plugin.<bean-name>.class=fully.qualified.ClassName
dev.langchain4j.cdi.plugin.<bean-name>.scope=jakarta.enterprise.context.ApplicationScoped
dev.langchain4j.cdi.plugin.<bean-name>.config.<property>=value
```

**Special lookup values:**
- `lookup:@default` = select default CDI bean
- `lookup:@all` = all beans of this type as a list
- `lookup:<name>` = named bean

The creator uses reflection to invoke the builder pattern (static `builder()` method + inner `*Builder` class).

## Testing

### Test Organization

**langchain4j-cdi-integration-tests-common**: Shared test resources and utilities

**langchain4j-cdi-integration-tests-quarkus**: Quarkus-specific integration tests
- Fast feedback with Quarkus dev mode
- Build-compatible extension validation

**langchain4j-cdi-integration-tests-jakartaee**: Jakarta EE server tests
- Tests for WildFly, GlassFish, Liberty, Payara
- Portable extension validation
- Uses Cargo Maven plugin for server management

**langchain4j-cdi-integration-tests-helidon**: Helidon framework tests

### Running Specific Test Modules

Run Quarkus integration tests:
```bash
mvn test -pl langchain4j-cdi-integration-tests/langchain4j-cdi-integration-tests-quarkus
```

Run Jakarta EE integration tests (note: longer execution time due to server startup):
```bash
mvn test -pl langchain4j-cdi-integration-tests/langchain4j-cdi-integration-tests-jakartaee
```

## Examples

All examples demonstrate a car booking application with chat, fraud detection, and function calling.

**Recommended for Development:** `examples/quarkus-car-booking`
- Fastest startup (~10 seconds)
- Dev mode support with live reload
- Best for rapid iteration

**Other Examples:**
- `examples/helidon-car-booking` and `examples/helidon-car-booking-portable-ext`
- `examples/glassfish-car-booking`
- `examples/liberty-car-booking`
- `examples/payara-car-booking` (currently broken - always exclude)

### Running Examples

1. Install all core modules first:
```bash
mvn clean install -DskipTests -pl '!examples/payara-car-booking'
```

2. Navigate to example directory and run:
```bash
cd examples/quarkus-car-booking
mvn quarkus:dev
```

3. Without Ollama, endpoints return connection errors (expected behavior)

4. With Ollama, use the provided setup scripts in the examples directory

## Code Quality

**Formatter:** Palantir Java Format (via Spotless)
- Style: PALANTIR
- Includes Javadoc formatting
- POM files are sorted with 4-space indentation

**Pre-commit Checks:**
```bash
mvn spotless:check verify -pl '!examples/payara-car-booking'
```

**Auto-fix Formatting:**
```bash
mvn spotless:apply -pl '!examples/payara-car-booking'
```

## Contributing Workflow

1. Build and test current state
2. Make changes in the appropriate module
3. Add unit tests in the same module
4. Add integration tests if the change affects runtime behavior
5. Format code: `mvn spotless:apply`
6. Validate: `mvn spotless:check verify -pl '!examples/payara-car-booking'`
7. Test with at least the Quarkus example application

**Backward Compatibility:** Required - use `@Deprecated` instead of removing fields/methods

**Avoid:**
- Lombok in new code (remove from old code if possible)
- New dependencies (check with `mvn dependency:analyze`)
- Breaking changes

## Common Patterns

### AI Service Creation Flow

1. `@RegisterAIService` annotated interface is detected by extension
2. Extension processes annotation metadata at startup (portable) or build time (build-compatible)
3. `CommonAIServiceCreator.create()` resolves CDI beans for each component
4. LangChain4j `AiServices.builder()` is populated with resolved components
5. Proxy implementation is created and registered as a CDI bean
6. Service is injected into application beans

### Configuration Loading

1. `LLMConfigProvider` SPI implementations scan for configuration sources
2. Properties matching `dev.langchain4j.plugin.*` or `langchain4j.*` are collected
3. `CommonLLMPluginCreator` uses reflection to instantiate builders
4. Builder methods are invoked with property values (type conversion handled)
5. Special `lookup:*` values trigger CDI bean resolution
6. Built instances are registered as CDI beans

## Troubleshooting

**Java Version Mismatch:** Ensure Java 21+ for builds, even though runtime target is Java 17

**Spotless Failures:** Run `mvn spotless:apply` before committing

**Payara Build Failures:** Always use `-pl '!examples/payara-car-booking'`

**Integration Test Timeouts:** Jakarta EE tests take longer due to server startup (30+ seconds per server)

**Missing Dependencies:** Run `mvn clean install` from repository root before working with examples
