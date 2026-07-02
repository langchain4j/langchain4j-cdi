package dev.langchain4j.cdi.agent;

/** Topology of an agentic system. */
public enum AgentTopologyType {
    /** A single agent with no orchestration. */
    SIMPLE,
    /** Agents executed in sequence. */
    SEQUENCE,
    /** Agents executed in a loop until a condition is met. */
    LOOP,
    /** Agents executed in parallel. */
    PARALLEL,
    /** Maps a list of items over a single sub-agent, executing each mapping in parallel. */
    PARALLEL_MAPPER,
    /** Routes to a sub-agent based on a condition. */
    CONDITIONAL,
    /** A supervisor agent that delegates to and coordinates sub-agents. */
    SUPERVISOR,
    /** A planner agent that decomposes tasks into steps. */
    PLANNER,
    /** An agent communicating via the Agent-to-Agent protocol. */
    A2A,
    /** Wraps an MCP server tool as an agent. */
    MCP_CLIENT,
    /** Injects a human-in-the-loop step that pauses execution and waits for human input. */
    HUMAN_IN_THE_LOOP
}
