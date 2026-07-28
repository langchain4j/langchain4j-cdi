---
title: AI Services
description: Declare and configure AI service interfaces with @RegisterAIService for CDI injection.
layout: page
---

# AI Services

AI services are the primary integration point between LangChain4j and CDI. You declare a Java interface annotated with `@RegisterAIService`, and the CDI extension creates a proxy implementation that delegates to LangChain4j's `AiServices` builder.

## Declaring an AI Service

```java
@RegisterAIService(
    chatModelName = "#default",
    toolProviderName = "myTools",
    chatMemoryName = "chat-memory",
    contentRetrieverName = "retriever"
)
public interface ChatAiService {
    String chat(String userMessage);
}
```

The proxy is registered as a CDI bean and can be injected into any CDI-managed bean.

## Annotation Attributes

| Attribute | Description |
|-----------|-------------|
| `chatModelName` | Name of the `ChatLanguageModel` CDI bean |
| `streamingChatModelName` | Name of the `StreamingChatLanguageModel` CDI bean |
| `toolProviderName` | Name of the `ToolProvider` CDI bean |
| `tools` | Array of tool classes (alternative to `toolProviderName`) |
| `chatMemoryName` | Name of the `ChatMemory` CDI bean |
| `chatMemoryProviderName` | Name of the `ChatMemoryProvider` CDI bean |
| `contentRetrieverName` | Name of the `ContentRetriever` CDI bean |
| `retrievalAugmentorName` | Name of the `RetrievalAugmentor` CDI bean |

## Name Resolution

Attribute values are resolved using these patterns:

- `"#default"` -- Use the default CDI bean of that type
- `""` (empty) -- Ignore this dependency
- `"myBean"` -- Use the CDI bean named `myBean`
- `"$\{property.key}"` -- Resolved via MicroProfile Config (requires `langchain4j-cdi-config`)
- `"#\{el.expression}"` -- Evaluated as Jakarta EL (requires `langchain4j-cdi-el`)

## Component Priority

When multiple component sources are specified:

- `RetrievalAugmentor` takes precedence over `ContentRetriever`
- `ToolProvider` is preferred over the `tools` array

## Configuration-Based Creation

Components can also be created from configuration properties:

```properties
dev.langchain4j.cdi.plugin.my-model.class=dev.langchain4j.model.openai.OpenAiChatModel
dev.langchain4j.cdi.plugin.my-model.scope=jakarta.enterprise.context.ApplicationScoped
dev.langchain4j.cdi.plugin.my-model.config.apiKey=$\{OPENAI_API_KEY}
dev.langchain4j.cdi.plugin.my-model.config.modelName=gpt-4o
```

### Special Lookup Values

- `lookup:@default` -- Select the default CDI bean
- `lookup:@all` -- All beans of this type as a list
- `lookup:<name>` -- Named bean lookup
