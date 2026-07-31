package dev.langchain4j.cdi.aiservice;

import java.time.Instant;

record DefaultThinkingEmitted(
        String text, String methodName, Class<?> serviceClass, Object memoryId, Instant capturedAt)
        implements ThinkingEmitted {}
