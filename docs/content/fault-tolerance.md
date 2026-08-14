---
title: Fault Tolerance
description: Add resilience to AI service calls with MicroProfile Fault Tolerance -- retry, timeout, circuit breaker, fallback.
layout: page
---

# Fault Tolerance

The `langchain4j-cdi-fault-tolerance` module integrates MicroProfile Fault Tolerance with your AI services. This enables resilient AI service calls with automatic retries, timeouts, circuit breakers, and fallback mechanisms.

## Setup

```xml
<dependency>
    <groupId>dev.langchain4j.cdi.mp</groupId>
    <artifactId>langchain4j-cdi-fault-tolerance</artifactId>
    <version>$\{langchain4j-cdi.version}</version>
</dependency>
```

## Retry

Automatically retry failed AI calls:

```java
@RegisterAIService(chatModelName = "#default")
public interface ChatBot {

    @Retry(maxRetries = 3, delay = 1000)
    String chat(String userMessage);
}
```

*Note*: LangChain4J has a built-in resiliency in its service. It's retry policy (enabled by default) is set to `maxRetries = 2`. 
So, should you include Microprofile `@Retry` policy, disable LangChain4J's `ChatModel`'s `maxRetries` by setting it to 0 so that you won't experience the `MxN` execution problem.


## Timeout

Set a maximum duration for AI calls:

```java
@RegisterAIService(chatModelName = "#default")
public interface ChatBot {

    @Timeout(5000)
    String chat(String userMessage);
}
```

## Circuit Breaker

Prevent cascading failures when the AI provider is down:

```java
@RegisterAIService(chatModelName = "#default")
public interface ChatBot {

    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
    String chat(String userMessage);
}
```

## Fallback

Provide alternative responses when the AI service is unavailable:

```java
@RegisterAIService(chatModelName = "#default")
public interface ChatBot {

    @Fallback(fallbackMethod = "chatFallback")
    String chat(String userMessage);

    default String chatFallback(String userMessage) {
        return "I'm sorry, the AI service is currently unavailable. Please try again later.";
    }
}
```

## Combining Strategies

Multiple fault tolerance strategies can be combined:

```java
@RegisterAIService(chatModelName = "#default")
public interface ResilientChatBot {

    @Retry(maxRetries = 3, delay = 500)
    @Timeout(10000)
    @CircuitBreaker(requestVolumeThreshold = 20, failureRatio = 0.5)
    @Fallback(fallbackMethod = "chatFallback")
    String chat(String userMessage);

    default String chatFallback(String userMessage) {
        return "Service temporarily unavailable.";
    }
}
```
