---
title: Telemetry
description: OpenTelemetry-based observability for AI service calls -- metrics, traces, and token usage.
layout: page
---

# Telemetry

The `langchain4j-cdi-telemetry` module integrates MicroProfile Telemetry (OpenTelemetry) with your AI services. It provides automatic instrumentation for request/response times, success/failure rates, and token usage.

## Setup

```xml
<dependency>
    <groupId>dev.langchain4j.cdi.mp</groupId>
    <artifactId>langchain4j-cdi-telemetry</artifactId>
    <version>$\{langchain4j-cdi.version}</version>
</dependency>
```

## What Gets Instrumented

The telemetry module automatically tracks:

- **Spans** for each AI service method invocation
- **Request duration** metrics
- **Token usage** (input/output/total tokens)
- **Success/failure rates**
- **GenAI exception semantics** following OpenTelemetry Semantic Conventions

## Semantic Conventions

The telemetry follows the [OpenTelemetry Semantic Conventions for GenAI](https://opentelemetry.io/docs/specs/semconv/gen-ai/):

| Attribute | Description |
|-----------|-------------|
| `gen_ai.system` | The AI system (e.g., `openai`) |
| `gen_ai.request.model` | Model name used for the request |
| `gen_ai.usage.input_tokens` | Number of input tokens |
| `gen_ai.usage.output_tokens` | Number of output tokens |
| `gen_ai.response.finish_reasons` | Why the model stopped generating |

## Configuration

OpenTelemetry configuration is done via standard MicroProfile Config / environment variables:

```properties
otel.exporter.otlp.endpoint=http://localhost:4317
otel.service.name=my-ai-service
otel.traces.exporter=otlp
otel.metrics.exporter=otlp
```
