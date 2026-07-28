---
title: Expression Language
description: Resolve annotation attribute values dynamically using Jakarta EL and MicroProfile Config expressions.
layout: page
---

# Expression Language

LangChain4j CDI supports dynamic resolution of annotation attribute values through an `ExpressionResolver` SPI. Two built-in implementations are provided:

| Syntax | Module | Resolver |
|--------|--------|----------|
| `#{...}` | `langchain4j-cdi-el` | Jakarta Expression Language |
| `${...}` | `langchain4j-cdi-config` | MicroProfile Config |

Both can coexist -- their delimiters are distinct, so there is no conflict.

## Where Expressions Are Supported

Expressions can be used in **any annotation attribute** that resolves a bean name or metadata value:

- **AI Service attributes** -- `chatModelName`, `streamingChatModelName`, `toolProviderName`, `chatMemoryName`, `chatMemoryProviderName`, `contentRetrieverName`, `retrievalAugmentorName`
- **Agent attributes** -- `name`, `description`, `outputKey`, `subAgentNames`, `chatModelName`, `agentListenerName`, `errorHandlerName`, `outputProviderName`, `beforeCallName`, and all other `*Name` attributes
- **Agent topology-specific attributes** -- `supervisorPromptName`, `exitConditionName`, `a2aServerUrl`, `mcpClientName`, `mcpToolNames`, `plannerName`, etc.

## Jakarta EL Expressions

### Setup

Add the `langchain4j-cdi-el` module:

```xml
<dependency>
    <groupId>dev.langchain4j.cdi</groupId>
    <artifactId>langchain4j-cdi-el</artifactId>
    <version>${langchain4j-cdi.version}</version>
</dependency>
```

In managed environments (WildFly, GlassFish, Liberty, Payara, Quarkus) a Jakarta EL runtime is already provided. In standalone or test scenarios, add an implementation explicitly:

```xml
<dependency>
    <groupId>org.glassfish.expressly</groupId>
    <artifactId>expressly</artifactId>
    <version>5.0.0</version>
    <scope>runtime</scope>
</dependency>
```

### How It Works

When `langchain4j-cdi-el` is on the classpath, the `JakartaELExpressionResolver` is discovered automatically via `java.util.ServiceLoader`. Any annotation attribute value matching `#{...}` (the expression must occupy the **entire** value) is evaluated as a Jakarta EL expression.

**CDI integration:** When CDI is active, `@Named` beans are directly accessible. For example, `#{myConfig.modelName}` calls `getModelName()` on the `@Named("myConfig")` bean.

**Non-CDI fallback:** When CDI is not active (e.g. during unit tests), basic EL evaluation still works: arithmetic, string operations, conditionals, and standard implicit objects like `systemProperties`.

**Failure handling:** If evaluation fails, the original `#{...}` value is returned unchanged and a `WARNING` is logged.

### Examples

**Conditional bean selection:**

```java
@RegisterAIService(chatModelName = "#{config.useGpu ? 'gpu-model' : 'cpu-model'}")
public interface SmartAssistant {
    String chat(String message);
}
```

**CDI bean property lookup:**

```java
@Named("agentConfig")
@ApplicationScoped
public class AgentConfig {
    public String getReviewerName() {
        return "reviewer-agent";
    }
}

@RegisterSimpleAgent(
    name = "#{agentConfig.reviewerName}",
    chatModelName = "#default"
)
public interface ReviewAgent {}
```

**System properties:**

```java
@RegisterAIService(chatModelName = "#{systemProperties['chat.model.bean']}")
public interface EnvironmentAwareService {
    String chat(String message);
}
```

**String operations and arithmetic:**

```java
@RegisterSimpleAgent(
    name = "#{'agent-' += systemProperties['app.env']}",
    chatModelName = "#default"
)
public interface EnvAgent {}
```

## MicroProfile Config Expressions

### Setup

Add the `langchain4j-cdi-config` module:

```xml
<dependency>
    <groupId>dev.langchain4j.cdi.mp</groupId>
    <artifactId>langchain4j-cdi-config</artifactId>
    <version>${langchain4j-cdi.version}</version>
</dependency>
```

### How It Works

The `MpConfigExpressionResolver` matches `${...}` values and resolves the enclosed key via `ConfigProvider.getConfig().getOptionalValue(key, String.class)`. If the key is not found, the original value is returned unchanged.

### Examples

**Externalize the model name:**

```properties
# microprofile-config.properties
ai.chat-model.name=my-openai-model
```

```java
@RegisterAIService(chatModelName = "${ai.chat-model.name}")
public interface ConfigDrivenService {
    String chat(String message);
}
```

**Configure agent sub-agents:**

```properties
pipeline.step1=step1-agent
```

```java
@RegisterSequenceAgent(
    name = "pipeline",
    subAgentNames = {"${pipeline.step1}", "step2", "step3"}
)
public interface PipelineAgent {}
```

## Combining Both Expression Types

Both resolvers can coexist in the same application. All registered `ExpressionResolver` implementations are applied as a pipeline: the output of each feeds into the next. Since `${...}` and `#{...}` use distinct delimiters, they never interfere with each other.

```java
// Resolved via MicroProfile Config
@RegisterAIService(chatModelName = "${my.chat.model.name}")
public interface ConfigService {
    String chat(String message);
}

// Resolved via Jakarta EL
@RegisterAIService(chatModelName = "#{runtimeConfig.chatModelName}")
public interface ElService {
    String chat(String message);
}
```

## Writing a Custom ExpressionResolver

The `ExpressionResolver` SPI is open for extension. Implement the interface and register it via `ServiceLoader` to support your own expression syntax.

### Interface

```java
package dev.langchain4j.cdi.spi;

public interface ExpressionResolver {
    /**
     * Resolves any expression embedded in value.
     * Must return value unchanged if not recognised.
     */
    String resolve(String value);
}
```

### Example: Vault Secret Resolver

```java
public class VaultExpressionResolver implements ExpressionResolver {

    @Override
    public String resolve(String value) {
        if (!value.startsWith("vault:")) {
            return value;
        }
        String secretPath = value.substring(6);
        return VaultClient.readSecret(secretPath);
    }
}
```

Register in `META-INF/services/dev.langchain4j.cdi.spi.ExpressionResolver`:

```
com.example.VaultExpressionResolver
```

### Pipeline Ordering

`ServiceLoader` does not guarantee a fixed discovery order. The built-in resolvers use distinct, non-overlapping delimiters, so their relative order does not matter. Custom resolvers whose output could match another resolver's pattern should use the standard `ServiceLoader` ordering mechanisms (e.g. module layers or explicit ordering in `module-info.java`).

## Summary of Name Resolution

All annotation attribute values go through this resolution chain before CDI bean lookup:

| Value | Resolution |
|-------|------------|
| `"#default"` | Default CDI bean of the expected type |
| `""` (empty) | Ignored -- dependency not wired |
| `"myBean"` | CDI bean named `myBean` |
| `"${property.key}"` | MicroProfile Config property lookup |
| `"#{el.expression}"` | Jakarta EL evaluation |
| `"vault:secret/path"` | Custom resolver (if registered) |
