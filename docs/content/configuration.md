---
title: Configuration
description: External configuration of LangChain4j CDI components via MicroProfile Config.
layout: page
---

# Configuration

The `langchain4j-cdi-config` module integrates with MicroProfile Config to externalize the configuration of AI components. This allows you to configure chat models, embedding models, memory providers, and other components without changing code.

## Setup

```xml
<dependency>
    <groupId>dev.langchain4j.cdi.mp</groupId>
    <artifactId>langchain4j-cdi-config</artifactId>
    <version>$\{langchain4j-cdi.version}</version>
</dependency>
```

## Configuration Properties

Properties follow the pattern:

```
langchain4j.<component>.<name>.<property>
```

### Chat Model Configuration

```properties
langchain4j.chat-model.default.provider=openai
langchain4j.chat-model.default.api-key=$\{OPENAI_API_KEY}
langchain4j.chat-model.default.model-name=gpt-4o
langchain4j.chat-model.default.temperature=0.7
```

### Plugin-Based Configuration

Components can be created entirely from configuration:

```properties
dev.langchain4j.cdi.plugin.my-model.class=dev.langchain4j.model.openai.OpenAiChatModel
dev.langchain4j.cdi.plugin.my-model.scope=jakarta.enterprise.context.ApplicationScoped
dev.langchain4j.cdi.plugin.my-model.config.apiKey=$\{OPENAI_API_KEY}
dev.langchain4j.cdi.plugin.my-model.config.modelName=gpt-4o
```

The plugin creator uses reflection to invoke the builder pattern (static `builder()` method + inner `*Builder` class).

### Special Lookup Values

Configuration values support CDI bean lookups:

| Value | Description |
|-------|-------------|
| `lookup:@default` | Select the default CDI bean |
| `lookup:@all` | All beans of this type as a list |
| `lookup:<name>` | Named bean lookup |

## Expression Resolution

Annotation attribute values support two expression types:

### MicroProfile Config Expressions

```java
@RegisterAIService(chatModelName = "$\{ai.chat-model.name}")
```

Requires the `langchain4j-cdi-config` module.

### Jakarta EL Expressions

```java
@RegisterAIService(chatModelName = "#\{configBean.chatModelName}")
```

Requires the `langchain4j-cdi-el` module and a Jakarta EL implementation (e.g., `org.glassfish.expressly:expressly`).

## LLMConfig SPI

For programmatic configuration, implement the `LLMConfigProvider` SPI:

```java
public class MyConfigProvider implements LLMConfigProvider {
    @Override
    public LLMConfig getConfig() {
        return LLMConfig.builder()
            // configure components
            .build();
    }
}
```
