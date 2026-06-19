package dev.langchain4j.cdi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.cdi.aiservice.CdiLookupHelper;
import dev.langchain4j.cdi.spi.RegisterA2AAgent;
import dev.langchain4j.cdi.spi.RegisterConditionalAgent;
import dev.langchain4j.cdi.spi.RegisterHumanInTheLoopAgent;
import dev.langchain4j.cdi.spi.RegisterLoopAgent;
import dev.langchain4j.cdi.spi.RegisterMcpClientAgent;
import dev.langchain4j.cdi.spi.RegisterParallelAgent;
import dev.langchain4j.cdi.spi.RegisterParallelMapperAgent;
import dev.langchain4j.cdi.spi.RegisterPlannerAgent;
import dev.langchain4j.cdi.spi.RegisterSequenceAgent;
import dev.langchain4j.cdi.spi.RegisterSimpleAgent;
import dev.langchain4j.cdi.spi.RegisterSupervisorAgent;
import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * Extracts CDI metadata from any of the 11 agent stereotype annotations: {@link RegisterSimpleAgent},
 * {@link RegisterSequenceAgent}, {@link RegisterLoopAgent}, {@link RegisterParallelAgent},
 * {@link RegisterParallelMapperAgent}, {@link RegisterConditionalAgent}, {@link RegisterSupervisorAgent},
 * {@link RegisterPlannerAgent}, {@link RegisterA2AAgent}, {@link RegisterMcpClientAgent}, and
 * {@link RegisterHumanInTheLoopAgent}.
 *
 * <p>Use {@link #detect(Class)} to obtain an instance from an interface class, or {@code null} when no recognized agent
 * annotation is present. Raw string fields ({@link #rawName()}, {@link #rawDescription()}, {@link #rawOutputKey()})
 * contain the annotation values before expression resolution; use {@link #name()}, {@link #description()}, and
 * {@link #outputKey()} to get the resolved values.
 *
 * <p><b>API note:</b> this record is part of the stable public API of {@code langchain4j-cdi-core}, alongside the
 * annotations in {@code dev.langchain4j.cdi.spi}. It lives in the {@code dev.langchain4j.cdi.agent} package for
 * historical reasons; treat it as public and stable.
 *
 * @param scope the CDI scope annotation class (e.g. {@code ApplicationScoped.class})
 * @param rawName the agent name before expression resolution
 * @param rawDescription the agent description before expression resolution
 * @param rawOutputKey the output key before expression resolution
 * @param rawTypedOutputKey the typed output key class, or {@code Agent.NoTypedKey.class} when not set
 * @param async whether the agent executes asynchronously
 * @param optional when {@code true} the agent's execution is silently skipped if any of its arguments is missing in the
 *     agentic scope, instead of failing the entire agentic system
 * @param rawSummarizedContext names of other agents whose conversation context should be summarized and injected into
 *     this agent's prompt, before expression resolution
 * @param annotationClass the concrete stereotype annotation class that was detected
 */
public record AgentAnnotationMeta(
        Class<? extends Annotation> scope,
        String rawName,
        String rawDescription,
        String rawOutputKey,
        Class<? extends TypedKey<?>> rawTypedOutputKey,
        boolean async,
        boolean optional,
        String[] rawSummarizedContext,
        Class<? extends Annotation> annotationClass) {

    /**
     * Detects which agent stereotype annotation is present on {@code interfaceClass} and returns the corresponding
     * meta. Returns {@code null} when no recognized annotation is found.
     *
     * <p><b>Sync note:</b> when adding a new topology annotation, update all four of:
     *
     * <ol>
     *   <li>this method
     *   <li>{@link CommonAgentCreator#create} dispatch chain
     *   <li>{@code hasAnyAgentAnnotation} in the build-compatible extension
     *   <li>the {@code @Enhancement}/{@code @WithAnnotations} arrays in the build-compatible extension
     * </ol>
     */
    public static AgentAnnotationMeta detect(Class<?> interfaceClass) {
        if (!interfaceClass.isInterface()) return null;
        RegisterSimpleAgent simple = interfaceClass.getAnnotation(RegisterSimpleAgent.class);
        if (simple != null)
            return new AgentAnnotationMeta(
                    simple.scope(),
                    simple.name(),
                    simple.description(),
                    simple.outputKey(),
                    simple.typedOutputKey(),
                    simple.async(),
                    simple.optional(),
                    simple.summarizedContext(),
                    RegisterSimpleAgent.class);

        RegisterSequenceAgent seq = interfaceClass.getAnnotation(RegisterSequenceAgent.class);
        if (seq != null)
            return new AgentAnnotationMeta(
                    seq.scope(),
                    seq.name(),
                    seq.description(),
                    seq.outputKey(),
                    seq.typedOutputKey(),
                    false,
                    seq.optional(),
                    seq.summarizedContext(),
                    RegisterSequenceAgent.class);

        RegisterLoopAgent loop = interfaceClass.getAnnotation(RegisterLoopAgent.class);
        if (loop != null)
            return new AgentAnnotationMeta(
                    loop.scope(),
                    loop.name(),
                    loop.description(),
                    loop.outputKey(),
                    loop.typedOutputKey(),
                    false,
                    loop.optional(),
                    loop.summarizedContext(),
                    RegisterLoopAgent.class);

        RegisterParallelAgent parallel = interfaceClass.getAnnotation(RegisterParallelAgent.class);
        if (parallel != null)
            return new AgentAnnotationMeta(
                    parallel.scope(),
                    parallel.name(),
                    parallel.description(),
                    parallel.outputKey(),
                    parallel.typedOutputKey(),
                    false,
                    parallel.optional(),
                    parallel.summarizedContext(),
                    RegisterParallelAgent.class);

        RegisterParallelMapperAgent mapper = interfaceClass.getAnnotation(RegisterParallelMapperAgent.class);
        if (mapper != null)
            return new AgentAnnotationMeta(
                    mapper.scope(),
                    mapper.name(),
                    mapper.description(),
                    mapper.outputKey(),
                    mapper.typedOutputKey(),
                    false,
                    mapper.optional(),
                    mapper.summarizedContext(),
                    RegisterParallelMapperAgent.class);

        RegisterConditionalAgent cond = interfaceClass.getAnnotation(RegisterConditionalAgent.class);
        if (cond != null)
            return new AgentAnnotationMeta(
                    cond.scope(),
                    cond.name(),
                    cond.description(),
                    cond.outputKey(),
                    cond.typedOutputKey(),
                    false,
                    cond.optional(),
                    cond.summarizedContext(),
                    RegisterConditionalAgent.class);

        RegisterSupervisorAgent supervisor = interfaceClass.getAnnotation(RegisterSupervisorAgent.class);
        if (supervisor != null)
            return new AgentAnnotationMeta(
                    supervisor.scope(),
                    supervisor.name(),
                    supervisor.description(),
                    supervisor.outputKey(),
                    supervisor.typedOutputKey(),
                    false,
                    supervisor.optional(),
                    supervisor.summarizedContext(),
                    RegisterSupervisorAgent.class);

        RegisterPlannerAgent planner = interfaceClass.getAnnotation(RegisterPlannerAgent.class);
        if (planner != null)
            return new AgentAnnotationMeta(
                    planner.scope(),
                    planner.name(),
                    planner.description(),
                    planner.outputKey(),
                    planner.typedOutputKey(),
                    false,
                    planner.optional(),
                    planner.summarizedContext(),
                    RegisterPlannerAgent.class);

        RegisterA2AAgent a2a = interfaceClass.getAnnotation(RegisterA2AAgent.class);
        if (a2a != null)
            return new AgentAnnotationMeta(
                    a2a.scope(),
                    a2a.name(),
                    a2a.description(),
                    a2a.outputKey(),
                    a2a.typedOutputKey(),
                    a2a.async(),
                    a2a.optional(),
                    a2a.summarizedContext(),
                    RegisterA2AAgent.class);

        RegisterMcpClientAgent mcp = interfaceClass.getAnnotation(RegisterMcpClientAgent.class);
        if (mcp != null)
            return new AgentAnnotationMeta(
                    mcp.scope(),
                    mcp.name(),
                    mcp.description(),
                    mcp.outputKey(),
                    mcp.typedOutputKey(),
                    mcp.async(),
                    mcp.optional(),
                    mcp.summarizedContext(),
                    RegisterMcpClientAgent.class);

        RegisterHumanInTheLoopAgent hitl = interfaceClass.getAnnotation(RegisterHumanInTheLoopAgent.class);
        if (hitl != null)
            return new AgentAnnotationMeta(
                    hitl.scope(),
                    hitl.name(),
                    hitl.description(),
                    hitl.outputKey(),
                    hitl.typedOutputKey(),
                    hitl.async(),
                    hitl.optional(),
                    hitl.summarizedContext(),
                    RegisterHumanInTheLoopAgent.class);

        return null;
    }

    /** Returns {@code true} when any recognized agent annotation is present on {@code interfaceClass}. */
    public static boolean isAgentInterface(Class<?> interfaceClass) {
        return detect(interfaceClass) != null;
    }

    /** Expression-resolved name, equivalent to the annotation's {@code name()} after EL/Config expansion. */
    public String name() {
        return CdiLookupHelper.resolveExpression(rawName);
    }

    /**
     * Expression-resolved description, equivalent to the annotation's {@code description()} after EL/Config expansion.
     */
    public String description() {
        return CdiLookupHelper.resolveExpression(rawDescription);
    }

    /** Expression-resolved output key, equivalent to the annotation's {@code outputKey()} after EL/Config expansion. */
    public String outputKey() {
        return CdiLookupHelper.resolveExpression(rawOutputKey);
    }

    /** Returns {@code true} when a typed output key class other than {@link Agent.NoTypedKey} is set. */
    public boolean hasTypedOutputKey() {
        return rawTypedOutputKey != null && rawTypedOutputKey != Agent.NoTypedKey.class;
    }

    /**
     * Expression-resolved summarized context agent names, equivalent to the annotation's {@code summarizedContext()}
     * after EL/Config expansion on each element.
     */
    public String[] summarizedContext() {
        if (rawSummarizedContext == null || rawSummarizedContext.length == 0) {
            return new String[0];
        }
        return Arrays.stream(rawSummarizedContext)
                .map(CdiLookupHelper::resolveExpression)
                .toArray(String[]::new);
    }
}
