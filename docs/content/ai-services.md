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
| `chatModelName` | Name of the `ChatModel` CDI bean |
| `streamingChatModelName` | Name of the `StreamingChatModel` CDI bean |
| `toolProviderName` | Name of the `ToolProvider` CDI bean |
| `tools` | Array of tool classes (alternative to `toolProviderName`) |
| `chatMemoryName` | Name of the `ChatMemory` CDI bean |
| `chatMemoryProviderName` | Name of the `ChatMemoryProvider` CDI bean |
| `contentRetrieverName` | Name of the `ContentRetriever` CDI bean |
| `retrievalAugmentorName` | Name of the `RetrievalAugmentor` CDI bean |
| `moderationModelName` | Name of the `ModerationModel` CDI bean |
| `inputGuardrails` | Array of `InputGuardrail` classes |
| `inputGuardrailNames` | Names of `InputGuardrail` CDI beans |
| `outputGuardrails` | Array of `OutputGuardrail` classes |
| `outputGuardrailNames` | Names of `OutputGuardrail` CDI beans |
| `listenerNames` | Names of `AiServiceListener` CDI beans |

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

## Guardrails

Guardrails validate messages before they are sent to the model (input) or before the response is returned (output). You can specify them by class or by CDI bean name:

```java
@RegisterAIService(
    inputGuardrails = \{NoEmptyMessageGuardrail.class},
    outputGuardrails = \{ContentFilterGuardrail.class}
)
public interface SafeChatService {
    String chat(String userMessage);
}
```

Or by named CDI bean:

```java
@RegisterAIService(
    inputGuardrailNames = {"noEmptyMessage"},
    outputGuardrailNames = {"contentFilter"}
)
public interface SafeChatService {
    String chat(String userMessage);
}
```

If both class arrays and name arrays are specified for the same guardrail direction, the classes take precedence and the names are ignored.

## AI Service Listeners

You can register `AiServiceListener` CDI beans on a specific AI service via the `listenerNames` attribute. Each listener receives lifecycle events (request issued, response received, errors, tool executions, etc.) scoped to that service only.

```java
@ApplicationScoped
@Named("chatLogger")
public class ChatLogger implements AiServiceResponseReceivedListener {

    @Override
    public void onEvent(AiServiceResponseReceivedEvent event) {
        System.out.printf("Method %s responded in context %s%n",
            event.invocationContext().methodName(),
            event.invocationContext().chatMemoryId());
    }
}
```

```java
@RegisterAIService(listenerNames = {"chatLogger"})
public interface ChatService {
    String chat(String userMessage);
}
```

Multiple listeners can be registered on the same service. The listener interfaces available from LangChain4j include:

| Listener Interface | Event Class | Description |
|-------------------|-------------|-------------|
| `AiServiceStartedListener` | `AiServiceStartedEvent` | AI service method invoked |
| `AiServiceRequestIssuedListener` | `AiServiceRequestIssuedEvent` | Chat request about to be sent |
| `AiServiceResponseReceivedListener` | `AiServiceResponseReceivedEvent` | Chat response received |
| `AiServiceCompletedListener` | `AiServiceCompletedEvent` | AI service method returned |
| `AiServiceErrorListener` | `AiServiceErrorEvent` | Error occurred |
| `ToolExecutedEventListener` | `ToolExecutedEvent` | Tool executed |
| `GuardrailExecutedListener` | `GuardrailExecutedEvent` | Guardrail executed |

## Capturing Model Thinking

Some models (Claude with extended thinking, OpenAI with reasoning, etc.) return "thinking" or reasoning content alongside their response. LangChain4j CDI provides two ways to capture this content.

### Using `@OnThinking` (inline handler)

Declare a `static void` method on the AI service interface annotated with `@OnThinking`. It is called after every non-streaming response that carries thinking content.

```java
@RegisterAIService
public interface MathAssistant {

    String solve(String problem);

    @OnThinking
    static void onThinking(ThinkingEmitted event) {
        System.out.printf("[%s] Thinking: %s%n",
            event.methodName(), event.text());
    }
}
```

The `ThinkingEmitted` event provides:

| Method | Description |
|--------|-------------|
| `text()` | The thinking/reasoning text |
| `methodName()` | The AI service method that produced it |
| `serviceClass()` | The AI service interface class |
| `memoryId()` | The chat memory ID (nullable) |
| `capturedAt()` | Timestamp of the event |

Constraints:
- The method must be `static` and return `void`
- It must accept a single `ThinkingEmitted` parameter
- Only one `@OnThinking` method is allowed per interface

### Using `listenerNames` (CDI bean)

For more flexibility, register a named CDI bean implementing `AiServiceResponseReceivedListener` and extract thinking from the response:

```java
@ApplicationScoped
@Named("thinkingCapture")
public class ThinkingCapture implements AiServiceResponseReceivedListener {

    @Override
    public void onEvent(AiServiceResponseReceivedEvent event) {
        String thinking = event.response().aiMessage().thinking();
        if (thinking != null && !thinking.isBlank()) {
            // log, store, forward, etc.
        }
    }
}
```

```java
@RegisterAIService(listenerNames = {"thinkingCapture"})
public interface MathAssistant {
    String solve(String problem);
}
```

This approach lets you reuse the same listener across multiple services, inject other CDI beans into it, and handle all event types -- not just thinking.

Both mechanisms can be combined on the same interface. Neither enables thinking on the model itself -- that is controlled by each model provider's configuration.
