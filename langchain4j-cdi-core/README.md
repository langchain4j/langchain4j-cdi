# langchain4j-cdi-core

This module provides the foundational building blocks needed to integrate LangChain4j with CDI (Jakarta EE, Quarkus, Helidon, etc.).
It includes:
- the stereotype annotation `@RegisterAIService` to declare an AI service interface;
- a utility `CommonAIServiceCreator` to build an AI Service proxy from CDI beans;
- the configuration API `LLMConfig` and its loader `LLMConfigProvider` (used by plugin creators in this module and sub-modules).

- It is used by the two types of CDI extensions:
- `langchain4j-cdi-portable-ext` for classic CDI;
- `langchain4j-cdi-build-compatible-ext` for static built CDI (Quarkus and Helidon, for example).

## When should you use this module?
- You want to expose a conversational service via a Java interface and have it dynamically implemented by LangChain4j.
- You want to assemble components (ChatModel, StreamingChatModel, ContentRetriever or RetrievalAugmentor, ToolProvider/Tools, ChatMemory/ChatMemoryProvider, ModerationModel) from CDI, by name or by default.

## Declare an AI Service with @RegisterAIService

```java
import dev.langchain4j.cdi.spi.RegisterAIService;

@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterAIService(
    chatModelName = "#default",           // selects the default ChatModel (if any)
    streamingChatModelName = "",          // optional
    contentRetrieverName = "",            // optional (mutually exclusive with retrievalAugmentorName)
    retrievalAugmentorName = "",          // optional (takes precedence over contentRetrieverName)
    toolProviderName = "myTools",         // or use tools = { MyTool.class }
    chatMemoryName = "",                  // optional
    chatMemoryProviderName = "",          // optional
    moderationModelName = ""              // optional
)
public interface ChatAiService {
    String chat(String question);
}
```

Notes:
- `"#default"` for a name means "select the default CDI bean" of that type.
- An empty/blank name means "ignore" the corresponding dependency.
- If `toolProviderName` is provided and resolvable, it is preferred. Otherwise, if the `tools` array is not empty, each class is instantiated via a no-args constructor and registered as a tool.

## Create the AI Service proxy

In most cases, this module is used by a CDI extension (see sub-modules) that automatically creates AI Service beans. If you need to do it manually (e.g., in a test), use:

```java
Instance<Object> lookup = ...; // provided by CDI
ChatAiService service = CommonAIServiceCreator.create(lookup, ChatAiService.class);
```

`CommonAIServiceCreator` reads the `@RegisterAIService` annotation on the interface and tries to resolve dependencies in CDI:
- ChatModel or StreamingChatModel;
- ContentRetriever or RetrievalAugmentor (RetrievalAugmentor has priority);
- ToolProvider or, if absent, tool classes listed in `tools`;
- ChatMemory or ChatMemoryProvider;
- ModerationModel.
Only the components that are resolved are wired into the `AiServices` builder.

## Configure plugins with LLMConfig

`LLMConfig` is a minimal API used to feed plugin creators from properties. Keys follow the prefix:

```
dev.langchain4j.plugin.<bean-name>.class=...               # target class (e.g., dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever)
dev.langchain4j.plugin.<bean-name>.scope=...               # CDI scope (default: ApplicationScoped)
dev.langchain4j.plugin.<bean-name>.config.<property>=...   # builder properties (e.g., api-key, endpoint, etc.)
```

Special values are supported:
- `lookup:@default` selects the default CDI bean of the expected type;
- `lookup:@all` returns all beans of this type as a list;
- `lookup:<name>` selects a named bean.

Creators (e.g., `CommonLLMPluginCreator`) reflect on the builder pattern used by LangChain4j classes (static `builder()` method + inner `*Builder` class) to populate the builder from properties.

## Examples

Declare a ContentRetriever from properties:

```
dev.langchain4j.plugin.content-retriever.class=dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
dev.langchain4j.plugin.content-retriever.scope=jakarta.enterprise.context.ApplicationScoped
dev.langchain4j.plugin.content-retriever.config.embedding-store=lookup:@default
dev.langchain4j.plugin.content-retriever.config.embedding-model=lookup:my-model
```

Then inject and reference this bean by name in `@RegisterAIService(contentRetrieverName = "content-retriever")`.

## Best practices
- Give explicit names to your CDI beans used by `@RegisterAIService`.
- Prefer `RetrievalAugmentor` over `ContentRetriever` when applicable (it is taken into account first).
- If you provide a `ToolProvider`, it will be preferred over listed tool classes.

## Key dependencies
- LangChain4j (AiServices, models, RAG, etc.)
- Jakarta CDI

## License
This project is licensed under the Apache 2.0 License. See the LICENSE file at the repository root.
