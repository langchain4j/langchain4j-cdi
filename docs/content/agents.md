---
title: Agent Orchestration
description: Wire multi-agent topologies using CDI annotations -- sequence, loop, parallel, supervisor, and more.
layout: page
---

# Agent Orchestration

LangChain4j CDI provides 11 per-topology stereotype annotations for wiring agents. Each annotation declares an agent with topology-specific behavior and shares a common set of attributes.

## Available Topologies

| Annotation | Description |
|-----------|-------------|
| `@RegisterSimpleAgent` | Single agent execution |
| `@RegisterSequenceAgent` | Sequential pipeline of sub-agents |
| `@RegisterLoopAgent` | Iterative agent execution with a condition |
| `@RegisterParallelAgent` | Concurrent execution of sub-agents |
| `@RegisterParallelMapperAgent` | Map input to parallel sub-agents |
| `@RegisterConditionalAgent` | Conditional routing to sub-agents |
| `@RegisterSupervisorAgent` | Supervisor-managed delegation |
| `@RegisterPlannerAgent` | Plan-and-execute orchestration |
| `@RegisterA2AAgent` | Agent-to-Agent protocol integration |
| `@RegisterMcpClientAgent` | MCP client consuming external servers |
| `@RegisterHumanInTheLoopAgent` | Human approval in agent flow |

## Common Attributes

All 11 topologies share these attributes:

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `scope` | `Class<? extends Annotation>` | `ApplicationScoped.class` | CDI scope |
| `name` | `String` | `""` | Agent name |
| `description` | `String` | `""` | Agent description |
| `outputKey` | `String` | `""` | Key for storing output in `AgenticScope` |
| `typedOutputKey` | `Class<? extends TypedKey<?>>` | `Agent.NoTypedKey.class` | Type-safe output key |
| `optional` | `boolean` | `false` | Skip silently when arguments are missing |
| `summarizedContext` | `String[]` | `{}` | Agent names whose context to summarize and inject |
| `agentListenerName` | `String` | `""` | CDI bean name of an `AgentListener` |

## Hook Attributes

Some topologies support callback hooks resolved as CDI beans by name:

| Attribute | Bean Type | Available On |
|-----------|-----------|------------|
| `errorHandlerName` | `Function<ErrorContext, ErrorRecoveryResult>` | Sequence, Loop, Parallel, ParallelMapper, Conditional, Planner, Supervisor |
| `outputProviderName` | `Function<AgenticScope, Object>` | Sequence, Loop, Parallel, ParallelMapper, Conditional, Planner, Supervisor |
| `beforeCallName` | `Consumer<AgenticScope>` | Sequence, Loop, Parallel, ParallelMapper, Conditional, Planner |

## Example: Sequence Agent

```java
@RegisterSequenceAgent(
    name = "pipeline",
    subAgentNames = {"step1", "step2", "step3"},
    errorHandlerName = "pipelineErrorHandler",
    beforeCallName = "pipelineSetup",
    outputProviderName = "pipelineOutput"
)
public interface PipelineAgent {}
```

## Example: Parallel Agent

```java
@RegisterParallelAgent(
    name = "research",
    subAgentNames = {"webSearch", "docSearch", "codeSearch"},
    outputKey = "researchResults"
)
public interface ResearchAgent {}
```

## Example: Conditional Agent

```java
@RegisterConditionalAgent(
    name = "router",
    routerName = "intentRouter",
    subAgentNames = {"faqAgent", "bookingAgent", "supportAgent"}
)
public interface RouterAgent {}
```
