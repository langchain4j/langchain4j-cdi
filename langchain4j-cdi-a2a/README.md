# Agentic Systems with LangChain4j CDI

The CDI integration provides a dedicated stereotype annotation for each agentic topology, enabling declarative registration of [LangChain4j Agentic](https://docs.langchain4j.dev/tutorials/agentic) systems as CDI beans. While `@RegisterAIService` creates simple AI services backed by a single LLM call, the agent annotations create agentic systems that can orchestrate multiple sub-agents in workflows such as sequences, loops, parallel execution, and supervisor-based delegation.

## Table of Contents

- [Dependencies](#dependencies)
- [Annotation Overview](#annotation-overview)
- [Common Attributes](#common-attributes)
- [Name Resolution](#name-resolution)
- [Topologies](#topologies)
- [Composing Agents](#composing-agents)
- [Differences from @RegisterAIService](#differences-from-registeraiservice)
- [The A2AAgentBuilder SPI](#the-a2aagentbuilder-spi)

## Dependencies

Add the agentic dependency alongside the CDI extension you already use:

```xml
<!-- Core agentic support -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-agentic</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

For **A2A (Agent-to-Agent) topology**, also add the A2A CDI integration module and the A2A protocol library:

```xml
<!-- CDI wiring for A2A topology -->
<dependency>
    <groupId>dev.langchain4j.cdi</groupId>
    <artifactId>langchain4j-cdi-a2a</artifactId>
    <version>${langchain4j-cdi.version}</version>
</dependency>

<!-- A2A protocol implementation -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-agentic-a2a</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

`langchain4j-cdi-a2a` is an optional module loaded via `ServiceLoader` when the A2A topology is requested. If it is absent from the classpath and `@RegisterA2AAgent` is used, an `IllegalStateException` is thrown at startup with a clear message.

## Annotation Overview

Each agentic topology has a dedicated CDI stereotype annotation. Use the annotation that matches the topology you want:

| Annotation | Topology | Purpose |
|---|---|---|
| `@RegisterSimpleAgent` | SIMPLE | Single agent backed by a chat model — tools, memory, retrieval, guardrails |
| `@RegisterSequenceAgent` | SEQUENCE | Executes sub-agents in order |
| `@RegisterLoopAgent` | LOOP | Repeats sub-agents until an exit condition is met or max iterations reached |
| `@RegisterParallelAgent` | PARALLEL | Executes sub-agents concurrently |
| `@RegisterParallelMapperAgent` | PARALLEL_MAPPER | Maps a list of items over a single sub-agent, executing each item in parallel |
| `@RegisterConditionalAgent` | CONDITIONAL | Routes to a sub-agent based on input conditions |
| `@RegisterSupervisorAgent` | SUPERVISOR | LLM-driven orchestrator that dynamically selects sub-agents |
| `@RegisterPlannerAgent` | PLANNER | Creates an explicit plan then executes it step-by-step |
| `@RegisterA2AAgent` | A2A | Connects to a remote agent via the A2A protocol over HTTP |
| `@RegisterMcpClientAgent` | MCP_CLIENT | Wraps an MCP server tool as an agent |
| `@RegisterHumanInTheLoopAgent` | HUMAN_IN_THE_LOOP | Pauses an agentic workflow to collect human input |

All annotations target interfaces and must be applied only to interfaces. They are CDI stereotypes that declare a default scope of `ApplicationScoped`.

## Common Attributes

These attributes are available on all agent annotations:

| Attribute | Default | Description |
|---|---|---|
| `name` | `""` | CDI bean name and agent logical name. When set, the bean is registered with `@Named` and can be referenced as a sub-agent by other agents |
| `description` | `""` | Agent description, used by supervisors and planners to select agents |
| `scope` | `ApplicationScoped.class` | CDI scope for the agent bean |
| `outputKey` | `""` | Key used to store this agent's output in the shared agentic scope |
| `async` | `false` | Whether the agent runs asynchronously (SIMPLE, A2A, and HUMAN_IN_THE_LOOP only) |
| `agentListenerName` | `""` | CDI bean name of an `AgentListener` for observability |
| `subAgentNames` | `{}` | CDI bean names of sub-agents (SEQUENCE, LOOP, PARALLEL, PARALLEL_MAPPER, CONDITIONAL, SUPERVISOR, PLANNER) |

## Name Resolution

Bean name resolution follows the same conventions as `@RegisterAIService`:

| Value | Behavior |
|---|---|
| `"#default"` | Use the default (unqualified) CDI bean |
| `""` (empty) | Ignore this dependency |
| `"my-bean"` | Use the bean with `@Named("my-bean")` |
| `"${prop.key}"` | Resolve via MicroProfile Config (requires `langchain4j-cdi-config`) |
| `"#{el.expression}"` | Evaluate as Jakarta EL expression (requires `langchain4j-cdi-el`) |

When `name` is set, the agent bean is automatically registered with that `@Named` qualifier, making it injectable by name without a separate `@Named` annotation on the interface.

String attributes that support expression resolution: `name`, `description`, `outputKey`, `chatModelName`, `streamingChatModelName`, `toolProviderName`, `chatMemoryName`, `chatMemoryProviderName`, `contentRetrieverName`, `retrievalAugmentorName`, `exitConditionName`, `exitConditionDescription`, `plannerName`, `supervisorContext`, `agentListenerName`, `a2aServerUrl`, `mcpClientName`, `mcpToolName`, `askUser`, and each element of `subAgentNames`.

## Topologies

### SIMPLE — `@RegisterSimpleAgent`

A single agent backed by a chat model. Full component wiring — tools, memory, retrieval, guardrails, streaming — is available. Equivalent to `@RegisterAIService` but with agent-specific features (output key, listener, agentic scope).

**Unique attributes:**

| Attribute | Default | Description |
|---|---|---|
| `chatModelName` | `"#default"` | CDI bean name of the `ChatModel`. `"#default"` uses the default bean |
| `streamingChatModelName` | `""` | CDI bean name of the `StreamingChatModel` |
| `toolNames` | `{}` | CDI bean names of `@Tool`-annotated beans |
| `toolProviderName` | `""` | CDI bean name of a `ToolProvider` (preferred over `toolNames`) |
| `chatMemoryName` | `""` | CDI bean name of the `ChatMemory` |
| `chatMemoryProviderName` | `""` | CDI bean name of the `ChatMemoryProvider` |
| `contentRetrieverName` | `""` | CDI bean name of a `ContentRetriever` for RAG |
| `retrievalAugmentorName` | `""` | CDI bean name of a `RetrievalAugmentor` (preferred over `contentRetrieverName`) |
| `inputGuardrailNames` | `{}` | CDI bean names implementing `InputGuardrail` |
| `outputGuardrailNames` | `{}` | CDI bean names implementing `OutputGuardrail` |

```java
@RegisterSimpleAgent(
    name = "style-editor",
    chatModelName = "ollama",
    toolNames = {"styleTools"},
    outputKey = "story")
public interface StyleEditor {

    @UserMessage("Rewrite the following story in {{style}} style: {{story}}")
    @Agent(description = "Edit a story to match a given style", outputKey = "story")
    String editStory(@V("story") String story, @V("style") String style);
}
```

### SEQUENCE — `@RegisterSequenceAgent`

Executes sub-agents one after another in order. Each agent can read previous agents' outputs from the shared scope via their output keys.

```java
@RegisterSequenceAgent(
    name = "content-pipeline",
    subAgentNames = {"researcher", "writer", "reviewer"},
    outputKey = "final-content")
public interface ContentPipeline {

    @Agent
    String produceContent(@V("topic") String topic);
}
```

### LOOP — `@RegisterLoopAgent`

Repeats sub-agents until an exit condition is met or `maxIterations` is reached.

**Unique attributes:**

| Attribute | Default | Description |
|---|---|---|
| `maxIterations` | `10` | Maximum number of iterations |
| `exitConditionName` | `""` | CDI bean name of a `Predicate<AgenticScope>` or `BiPredicate<AgenticScope, Integer>` that triggers early exit. When both `Predicate` and `BiPredicate` beans are registered under the same name, `Predicate` takes precedence |
| `exitConditionDescription` | `""` | Human-readable description of the exit condition, forwarded to the loop builder |
| `testAfterEachIteration` | `false` | When `true`, the exit condition is evaluated after each iteration completes rather than before |

When `exitConditionName` is blank, the loop runs until `maxIterations` is exhausted.

```java
// Exit condition bean
@ApplicationScoped
@Named("qualityCheck")
public class QualityCheck implements Predicate<AgenticScope> {

    @Override
    public boolean test(AgenticScope scope) {
        Object score = scope.readState("score");
        return score instanceof Number n && n.doubleValue() >= 0.8;
    }
}

// Loop agent
@RegisterLoopAgent(
    name = "refinement-loop",
    subAgentNames = {"scorer", "editor"},
    maxIterations = 5,
    exitConditionName = "qualityCheck",
    exitConditionDescription = "Stop when quality score reaches 0.8",
    testAfterEachIteration = true)
public interface RefinementLoop {

    @Agent
    String refine(@V("draft") String draft);
}
```

### PARALLEL — `@RegisterParallelAgent`

Executes all sub-agents concurrently and merges their results.

```java
@RegisterParallelAgent(
    name = "multi-analyst",
    subAgentNames = {"technical-analyst", "business-analyst", "risk-analyst"},
    outputKey = "analysis")
public interface MultiAnalyst {

    @Agent
    String analyze(@V("proposal") String proposal);
}
```

### PARALLEL_MAPPER — `@RegisterParallelMapperAgent`

Maps a list of items read from the agentic scope over a single sub-agent, executing each mapping in parallel.

**Unique attributes:**

| Attribute | Default | Description |
|---|---|---|
| `itemsKey` | *(required)* | Key in `AgenticScope` that holds the list of items to map over |
| `subAgentNames` | `{}` | The first entry is the worker agent applied to each item |

```java
@RegisterParallelMapperAgent(
    name = "batch-translator",
    subAgentNames = {"translator"},
    itemsKey = "texts",
    outputKey = "translations")
public interface BatchTranslator {

    @Agent
    String translateAll(@V("language") String language);
}
```

### CONDITIONAL — `@RegisterConditionalAgent`

Routes to a specific sub-agent based on input conditions.

```java
@RegisterConditionalAgent(
    name = "router",
    subAgentNames = {"support-agent", "sales-agent", "billing-agent"})
public interface RequestRouter {

    @Agent
    String route(@V("request") String request);
}
```

### SUPERVISOR — `@RegisterSupervisorAgent`

An LLM-driven orchestrator that dynamically selects which sub-agents to invoke based on the task. Requires a `ChatModel`. Uses agent descriptions to make routing decisions.

**Unique attributes:**

| Attribute | Default | Description |
|---|---|---|
| `chatModelName` | `"#default"` | CDI bean name of the `ChatModel` |
| `chatMemoryProviderName` | `""` | CDI bean name of a `ChatMemoryProvider` |
| `maxAgentsInvocations` | `10` | Maximum number of sub-agent invocations |
| `supervisorContext` | `""` | Static system context string injected into the supervisor's prompt |

**Note:** SUPERVISOR supports `chatMemoryProviderName` (a `ChatMemoryProvider` that creates memory per session). It does **not** support a single shared `ChatMemory` instance — use `chatMemoryProviderName` when the supervisor needs memory.

```java
@RegisterSupervisorAgent(
    name = "project-manager",
    chatModelName = "gpt4",
    chatMemoryProviderName = "per-session-memory",
    subAgentNames = {"developer", "tester", "documenter"},
    maxAgentsInvocations = 15,
    supervisorContext = "You are a project manager coordinating a software team. Always prefer the developer for implementation tasks and the tester for validation.")
public interface ProjectManager {

    @Agent
    String manageTask(@V("task") String task);
}
```

`supervisorContext` supports expression resolution, so the value can be externalised:

```java
@RegisterSupervisorAgent(
    name = "domain-supervisor",
    subAgentNames = {"agentA", "agentB"},
    supervisorContext = "${supervisor.system.context}")
public interface DomainSupervisor {
    @Agent String dispatch(@V("task") String task);
}
```

### PLANNER — `@RegisterPlannerAgent`

Similar to SUPERVISOR but creates an explicit plan before execution, then follows the plan step by step. A `Planner` must be supplied — either by naming a CDI bean via `plannerName`, or by declaring a static `@PlannerSupplier` method directly on the interface.

**Unique attributes:**

| Attribute | Default | Description |
|---|---|---|
| `plannerName` | `""` | CDI bean name of a `Planner` (required unless a `@PlannerSupplier` method is declared on the interface) |

**Option 1 — CDI bean via `plannerName`:**

```java
@RegisterPlannerAgent(
    name = "project-planner",
    plannerName = "myPlanner",
    subAgentNames = {"researcher", "developer", "reviewer"})
public interface ProjectPlanner {

    @Agent
    String executeProject(@V("requirements") String requirements);
}
```

`myPlanner` must resolve to a CDI `Planner` bean annotated with `@Named("myPlanner")`.

**Option 2 — inline `@PlannerSupplier` method:**

```java
@RegisterPlannerAgent(
    name = "project-planner",
    subAgentNames = {"researcher", "developer", "reviewer"})
public interface ProjectPlanner {

    @Agent
    String executeProject(@V("requirements") String requirements);

    @PlannerSupplier
    static Planner providePlanner() {
        return new MyCustomPlanner();
    }
}
```

If neither `plannerName` nor a `@PlannerSupplier` method is present, the CDI deployment fails with an `IllegalArgumentException` at startup.

### A2A (Agent-to-Agent Protocol) — `@RegisterA2AAgent`

Connects to a remote agent via the [A2A protocol](https://google.github.io/A2A/). The remote agent runs as a separate service and is accessed over HTTP. No local `ChatModel` is needed. Requires `langchain4j-cdi-a2a` on the classpath (see [Dependencies](#dependencies)).

**Unique attributes:**

| Attribute | Default | Description |
|---|---|---|
| `a2aServerUrl` | *(required)* | URL of the A2A server. Supports expression resolution |

```java
@RegisterA2AAgent(
    name = "creative-writer",
    a2aServerUrl = "http://remote-agent-host:8080",
    outputKey = "story")
public interface RemoteWriter {

    @Agent(description = "Generate a story based on the given topic", outputKey = "story")
    String generateStory(@V("topic") String topic);
}
```

`a2aServerUrl` supports expression resolution, so the URL can be externalised:

```java
@RegisterA2AAgent(
    name = "remote-summarizer",
    a2aServerUrl = "${remote.summarizer.url}")
public interface RemoteSummarizer {
    @Agent String summarize(@V("text") String text);
}
```

### MCP_CLIENT — `@RegisterMcpClientAgent`

Wraps a single MCP server tool as an agent. The tool is invoked as a step within an agentic workflow.

**Unique attributes:**

| Attribute | Default | Description |
|---|---|---|
| `mcpClientName` | *(required)* | CDI bean name of the `McpClient` |
| `mcpToolName` | *(required)* | Name of the MCP tool to invoke |
| `mcpInputKeys` | `{}` | Keys of input arguments read from `AgenticScope` |

```java
@RegisterMcpClientAgent(
    name = "web-searcher",
    mcpClientName = "myMcpClient",
    mcpToolName = "web_search",
    mcpInputKeys = {"query"},
    outputKey = "searchResults")
public interface WebSearcher {
    @Agent String search(@V("query") String query);
}
```

### HUMAN_IN_THE_LOOP — `@RegisterHumanInTheLoopAgent`

Pauses an agentic workflow to collect human input. The interface declares a static method that accepts a single `AgenticScope` parameter and returns a `String`. This method is called to obtain the human response.

**Unique attributes:**

| Attribute | Default | Description |
|---|---|---|
| `askUser` | `""` | Name of the static method that provides the human response. When empty, the framework selects the only matching static `String(AgenticScope)` method automatically |

```java
@RegisterHumanInTheLoopAgent(
    name = "approval-gate",
    outputKey = "approval")
public interface ApprovalGate {

    static String askUser(AgenticScope scope) {
        // prompt the user and return their response
        return "approved";
    }

    String requestApproval(@V("proposal") String proposal);
}
```

When the interface declares multiple matching static methods, use `askUser` to select the right one:

```java
@RegisterHumanInTheLoopAgent(
    name = "approval-gate",
    askUser = "promptHuman",
    outputKey = "approval")
public interface ApprovalGate {

    static String promptHuman(AgenticScope scope) {
        return "approved";
    }

    static String formatMessage(AgenticScope scope) {
        return scope.get("message").toString();
    }

    String requestApproval(@V("proposal") String proposal);
}
```

## Composing Agents

Agent beans can be composed into larger workflows by referencing them as sub-agents via their `name`:

```java
// Step 1: A remote A2A agent that writes stories
@RegisterA2AAgent(
    name = "creative-writer",
    a2aServerUrl = "http://localhost:8080",
    outputKey = "story")
public interface CreativeWriter {
    @Agent(description = "Generate a Norse saga", outputKey = "story")
    String generateStory(@V("topic") String topic);
}

// Step 2: A local agent that edits stories
@RegisterSimpleAgent(
    name = "style-editor",
    chatModelName = "ollama",
    outputKey = "story")
public interface StyleEditor {
    @UserMessage("Rewrite this saga in {{style}} style: {{story}}")
    @Agent(description = "Edit a saga to match a style", outputKey = "story")
    String editStory(@V("story") String story, @V("style") String style);
}

// Step 3: A sequence that chains them together
@RegisterSequenceAgent(
    subAgentNames = {"creative-writer", "style-editor"},
    outputKey = "story")
public interface StyledWriter {
    @Agent
    String writeStoryWithStyle(@V("topic") String topic, @V("style") String style);
}
```

Sub-agents can be any combination of CDI agent beans and manually produced CDI beans (via `@Produces @Named`). This is useful for agents that need programmatic configuration, such as a loop with a custom exit condition:

```java
@ApplicationScoped
public class AgentProducers {

    @Inject @Named("scorer") Scorer scorer;
    @Inject StyleEditor styleEditor;

    @Produces
    @Named("review-loop")
    public UntypedAgent reviewLoop() {
        return AgenticServices.loopBuilder()
                .subAgents(
                    CommonAgentCreator.toAgentExecutor(scorer),
                    CommonAgentCreator.toAgentExecutor(styleEditor))
                .maxIterations(5)
                .exitCondition(scope -> {
                    Object score = scope.readState("score");
                    return score instanceof Number n && n.doubleValue() >= 0.8;
                })
                .build();
    }
}
```

**Important:** When passing CDI agent beans directly to programmatic agentic builders (like `loopBuilder().subAgents(...)`), wrap them with `CommonAgentCreator.toAgentExecutor()`. CDI client proxies (used for `@ApplicationScoped` beans) lose method annotations, preventing the agentic framework from finding the agent's entry point. This wrapper inspects the original interface instead. This is **not** needed when using `subAgentNames` — the framework handles it automatically.

## Differences from @RegisterAIService

| Feature | `@RegisterAIService` | Agent annotations |
|---|---|---|
| Default scope | `RequestScoped` | `ApplicationScoped` |
| Moderation model | Supported | Not supported |
| Agent topologies | N/A | 11 topologies (SIMPLE, SEQUENCE, LOOP, PARALLEL, PARALLEL_MAPPER, CONDITIONAL, SUPERVISOR, PLANNER, A2A, MCP_CLIENT, HUMAN_IN_THE_LOOP) |
| Sub-agent orchestration | N/A | Via `subAgentNames` |
| Output key / agentic scope | N/A | Supported |
| Agent listener | N/A | Via `agentListenerName` |
| CDI bean naming | Requires separate `@Named` | Built-in via `name` attribute |
| A2A protocol | N/A | Via `@RegisterA2AAgent` |

## The A2AAgentBuilder SPI

The A2A topology is loaded via an SPI so the `langchain4j-cdi-a2a` dependency remains optional. The `A2AAgentBuilder` interface (`dev.langchain4j.cdi.agent.spi.A2AAgentBuilder`) defines the contract:

```java
public interface A2AAgentBuilder {
    <X> X build(
            Class<X> interfaceClass,
            String url,
            String outputKey,
            boolean async,
            String agentListenerName,
            Instance<Object> lookup);
}
```

`DefaultA2AAgentBuilder` (in this module) is the standard implementation, registered in `META-INF/services/dev.langchain4j.cdi.agent.spi.A2AAgentBuilder`. It validates that `url` is non-blank, then wires `outputKey`, `async`, and `agentListenerName` onto the A2A client builder. Expression resolution (`${...}` and `#{...}`) is applied by `CommonAgentCreator` before the SPI is called.

You can provide a custom `A2AAgentBuilder` implementation (e.g. to add custom HTTP configuration or authentication) by registering it as a ServiceLoader service. If multiple implementations are on the classpath, the first one found is used — the standard Java `ServiceLoader` discovery order applies.

## License

Apache License 2.0 — see the [LICENSE](../LICENSE) file.
