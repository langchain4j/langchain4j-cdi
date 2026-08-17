---
layout: post
title: 'LangChain4j CDI 1.4.0 Released'
date: 2026-08-17
tags: release langchain4j cdi
synopsis: 'LangChain4j CDI 1.4.0 ships thinking capture support, GenAI telemetry improvements, a full MCP Java 1.x migration, and LangChain4j 1.19.0.'
author: ehsavoie
---

LangChain4j CDI 1.4.0 is available. This release brings thinking capture for AI services, improvements to GenAI telemetry semantics, a full migration to MCP Java 1.x, and a module-system fix for build-time extensions.

## What's New

### Thinking Capture for AI Services

AI service methods can now capture the model's internal reasoning — the "thinking" tokens emitted by reasoning models like Claude. This is exposed via a new listener/guardrail hook, so you can observe or act on the thought chain alongside the final response.

### GenAI Telemetry Improvements

The `langchain4j-cdi-telemetry` module now follows the OpenTelemetry [GenAI Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/gen-ai/) more completely, including correct exception semantics. If a model call raises an error, the resulting span is now attributed according to the GenAI spec rather than the generic HTTP conventions.

### MCP Java 1.x Migration

The MCP server module tree has been migrated to **MCP Java 1.x** (`mcpjava 1.x`). This is a breaking change within the MCP sub-tree: package names and some API surface were reorganised as part of the 1.x overhaul. If you depend on `langchain4j-cdi-mcp-*` directly, check the [MCP Server]({site.url('mcp-server')}) guide and the upstream MCP Java changelog for the new package names.

### JPMS Split-Package Fix

A split-package conflict in the build-compatible extension (`langchain4j-cdi-build-compatible-ext`) has been resolved. Applications running on runtimes that enforce Java module boundaries (JPMS) -- such as Quarkus with strict module mode -- will no longer encounter split-package errors at startup.

## Dependency Upgrades

| Dependency | From | To |
|---|---|---|
| LangChain4j | 1.18.0 | 1.19.0 |
| `resteasy-bom` | 6.2.16.Final | 6.2.17.Final |
| `yasson` | 3.0.4 | 3.0.5 |
| `cargo-maven3-plugin` | 1.10.27 | 1.10.28 |
| `apache-maven` | 3.9.11 | 3.9.16 |
| `maven-wrapper` | 3.3.2 | 3.3.4 |

## Upgrading

Update your dependency version to `1.4.0`:

```xml
<properties>
    <langchain4j-cdi.version>1.4.0</langchain4j-cdi.version>
</properties>
```

If you use the MCP server module, review the [MCP Server]({site.url('mcp-server')}) documentation and verify your import statements after the mcpjava 1.x package rename.

## Contributors

Thank you to everyone who contributed to this release:

- **@ehsavoie** -- thinking capture, MCP migration, JPMS fix, and release coordination
- **@TheEliteGentleman** -- telemetry improvements and documentation
- **@dependabot** -- automated dependency upgrades

Full release notes and the complete list of merged pull requests are available on the [GitHub Releases page](https://github.com/langchain4j/langchain4j-cdi/releases/tag/1.4.0).
