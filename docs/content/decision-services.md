---
title: Decision Services
description: Declare and configure decision service interfaces with @RegisterDecisionService for CDI injection.
layout: page
---

# Decision Services

Decision services are the primary integration point between LangChain4j and CDI. You declare a Java interface annotated with `@RegisterDecisionService`, and the CDI extension creates a proxy implementation that delegates to LangChain4j's `DecisionServices` builder.

## Declaring an AI Service

```java
@RegisterDecisionService(
    decisionModelName = "#default"
)
public interface SupportDesk {
	@Decide("Which intent best describes the user's request?")
	Choice<Intent> classifyIntent(String ticketText);

	@Decide("Is this customer message considered spam?")
	boolean isSpam(String ticketText);
}
```

The proxy is registered as a CDI bean and can be injected into any CDI-managed bean.

## Annotation Attributes

| Attribute | Description |
|-----------|-------------|
| `decisionModelName` | Name of the `DecisionModel` CDI bean |
| `thresholdProviderName` | Name of the `ThresholdProvider` CDI bean |

## Name Resolution

Attribute values are resolved using these patterns:

- `"#default"` -- Use the default CDI bean of that type
- `""` (empty) -- Ignore this dependency
- `"myBean"` -- Use the CDI bean named `myBean`
- `"$\{property.key}"` -- Resolved via MicroProfile Config (requires `langchain4j-cdi-config`)
- `"#\{el.expression}"` -- Evaluated as Jakarta EL (requires `langchain4j-cdi-el`)

## Configuration-Based Creation

Components can also be created from configuration properties:

```properties
# Using TypeSafe AI Jev Decision Model
dev.langchain4j.cdi.plugin.my-decision-model.class=dev.langchain4j.model.typesafe.TypeSafeDecisionModel
dev.langchain4j.cdi.plugin.my-decision-model.scope=jakarta.enterprise.context.ApplicationScoped
dev.langchain4j.cdi.plugin.my-decision-model.config.apiKey=$\{TYPESAFE_AI_API_KEY}
dev.langchain4j.cdi.plugin.my-decision-model.config.modelName=jev-latest
```

This requires that you import `langchain4j-typesafe` module.

### Special Lookup Values

- `lookup:@default` -- Select the default CDI bean
- `lookup:@all` -- All beans of this type as a list
- `lookup:<name>` -- Named bean lookup


