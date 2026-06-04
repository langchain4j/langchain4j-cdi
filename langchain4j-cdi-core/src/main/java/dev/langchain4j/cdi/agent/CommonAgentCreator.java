package dev.langchain4j.cdi.agent;

import static dev.langchain4j.cdi.aiservice.CdiLookupHelper.hasText;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.agent.AgentBuilder;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.internal.AgentInvoker;
import dev.langchain4j.agentic.internal.AgentUtil;
import dev.langchain4j.agentic.internal.AgenticScopeOwner;
import dev.langchain4j.agentic.internal.InternalAgent;
import dev.langchain4j.agentic.internal.McpService;
import dev.langchain4j.agentic.internal.NonAiAgentInstance;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.planner.AgentArgument;
import dev.langchain4j.agentic.planner.AgenticService;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.planner.PlannerBasedService;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorAgentService;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.agentic.workflow.LoopAgentService;
import dev.langchain4j.agentic.workflow.ParallelMapperService;
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
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utility to build LangChain4j agent proxies from CDI beans and agent stereotype annotation metadata.
 *
 * <p>Supports all 11 per-topology agent stereotype annotations: {@link RegisterSimpleAgent},
 * {@link RegisterSequenceAgent}, {@link RegisterLoopAgent}, {@link RegisterParallelAgent},
 * {@link RegisterParallelMapperAgent}, {@link RegisterConditionalAgent}, {@link RegisterSupervisorAgent},
 * {@link RegisterPlannerAgent}, {@link RegisterA2AAgent}, {@link RegisterMcpClientAgent}, and
 * {@link RegisterHumanInTheLoopAgent}.
 */
public class CommonAgentCreator {

    private static final Logger LOGGER = Logger.getLogger(CommonAgentCreator.class.getName());

    /** CDI bean name prefix for agents that have no explicit name set in their annotation. */
    public static final String AGENT_BEAN_NAME_PREFIX = "registeredAgent-";

    private static final String AGENT_TOSTRING_PREFIX = "Agent[";

    public static <X> X create(Instance<Object> lookup, Class<X> interfaceClass) {
        RegisterSimpleAgent simple = interfaceClass.getAnnotation(RegisterSimpleAgent.class);
        if (simple != null) return createForSimple(lookup, interfaceClass, simple);

        RegisterSequenceAgent sequence = interfaceClass.getAnnotation(RegisterSequenceAgent.class);
        if (sequence != null) return createForSequence(lookup, interfaceClass, sequence);

        RegisterLoopAgent loop = interfaceClass.getAnnotation(RegisterLoopAgent.class);
        if (loop != null) return createForLoop(lookup, interfaceClass, loop);

        RegisterParallelAgent parallel = interfaceClass.getAnnotation(RegisterParallelAgent.class);
        if (parallel != null) return createForParallel(lookup, interfaceClass, parallel);

        RegisterParallelMapperAgent mapper = interfaceClass.getAnnotation(RegisterParallelMapperAgent.class);
        if (mapper != null) return createForParallelMapper(lookup, interfaceClass, mapper);

        RegisterConditionalAgent conditional = interfaceClass.getAnnotation(RegisterConditionalAgent.class);
        if (conditional != null) return createForConditional(lookup, interfaceClass, conditional);

        RegisterSupervisorAgent supervisor = interfaceClass.getAnnotation(RegisterSupervisorAgent.class);
        if (supervisor != null) return createForSupervisor(lookup, interfaceClass, supervisor);

        RegisterPlannerAgent planner = interfaceClass.getAnnotation(RegisterPlannerAgent.class);
        if (planner != null) return createForPlanner(lookup, interfaceClass, planner);

        RegisterA2AAgent a2a = interfaceClass.getAnnotation(RegisterA2AAgent.class);
        if (a2a != null) return createForA2A(lookup, interfaceClass, a2a);

        RegisterMcpClientAgent mcp = interfaceClass.getAnnotation(RegisterMcpClientAgent.class);
        if (mcp != null) return createForMcpClient(lookup, interfaceClass, mcp);

        RegisterHumanInTheLoopAgent hitl = interfaceClass.getAnnotation(RegisterHumanInTheLoopAgent.class);
        if (hitl != null) return createForHumanInTheLoop(lookup, interfaceClass, hitl);

        throw new IllegalArgumentException("Interface " + interfaceClass.getName()
                + " must be annotated with a per-topology stereotype"
                + " (@RegisterSimpleAgent, @RegisterLoopAgent, etc.)");
    }

    // =========================================================================
    // createForXxx — per-topology dispatchers
    // =========================================================================

    @SuppressWarnings("unchecked")
    private static <X> X createForSimple(Instance<Object> lookup, Class<X> interfaceClass, RegisterSimpleAgent ann) {
        ChatModel chatModel = CdiLookupHelper.resolveSingle(lookup, ChatModel.class, ann.chatModelName());
        StreamingChatModel streamingChatModel =
                CdiLookupHelper.resolveSingle(lookup, StreamingChatModel.class, ann.streamingChatModelName());

        Method entryMethod = findEntryMethod(interfaceClass);
        if (entryMethod != null && entryMethod.isAnnotationPresent(Agent.class)) {
            return AgenticServices.createAgenticSystem(
                    interfaceClass,
                    chatModel,
                    new AgenticServices.AgentConfigurator(
                            ctx -> {
                                var builder = ctx.agentBuilder();
                                if (streamingChatModel != null) {
                                    builder.streamingChatModel(streamingChatModel);
                                }
                                AgentComponents.resolve(ann, lookup, interfaceClass.getSimpleName())
                                        .applyTo(builder);
                                String resolvedName = CdiLookupHelper.resolveExpression(ann.name());
                                if (hasText(resolvedName)) {
                                    builder.name(resolvedName);
                                }
                                String resolvedDescription = CdiLookupHelper.resolveExpression(ann.description());
                                if (hasText(resolvedDescription)) {
                                    builder.description(resolvedDescription);
                                }
                                String resolvedOutputKey = CdiLookupHelper.resolveExpression(ann.outputKey());
                                if (hasText(resolvedOutputKey)) {
                                    builder.outputKey(resolvedOutputKey);
                                }
                                builder.async(ann.async());
                                applyListener(builder::listener, ann.agentListenerName(), lookup);
                            },
                            agentClass -> {
                                Instance<?> instance = lookup.select(agentClass);
                                return instance.isResolvable() ? instance.get() : null;
                            }));
        }

        X aiService = buildAiServiceForSimple(interfaceClass, ann, chatModel, streamingChatModel, lookup);
        String description = CdiLookupHelper.resolveExpression(ann.description());
        if (!hasText(description)) description = "";
        String outputKey = CdiLookupHelper.resolveExpression(ann.outputKey());
        if (!hasText(outputKey)) outputKey = "";
        AgentListener listener = CdiLookupHelper.resolveSingle(lookup, AgentListener.class, ann.agentListenerName());
        NonAiAgentInstance agentInstance = buildNonAiAgentInstance(
                interfaceClass, entryMethod, ann.name(), description, outputKey, ann.async(), listener);
        InvocationHandler handler = (proxy, method, args) -> {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> AGENT_TOSTRING_PREFIX + agentInstance.name() + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> method.invoke(aiService, args);
                };
            }
            // InternalAgent extends AgentInstance, so methods declared on AgentInstance (name,
            // outputKey, etc.) have declaringClass == AgentInstance. The reversed direction
            // "declaringClass.isAssignableFrom(InternalAgent.class)" returns true for both
            // InternalAgent and its supertypes, routing the full internal-method hierarchy here.
            if (declaringClass.isAssignableFrom(InternalAgent.class)) {
                return method.invoke(agentInstance, args);
            }
            // Simple AI-service agents have no planner scope. When AgentExecutor calls
            // withAgenticScope() via the Weld scope proxy (which now exposes AgenticScopeOwner),
            // return this proxy unchanged so execution continues without binding a child scope.
            if (declaringClass.isAssignableFrom(AgenticScopeOwner.class)) {
                return method.getName().equals("withAgenticScope") ? proxy : null;
            }
            return method.invoke(aiService, args);
        };
        return AgentUtil.buildAgent(interfaceClass, handler);
    }

    private static <X> X buildAiServiceForSimple(
            Class<X> interfaceClass,
            RegisterSimpleAgent ann,
            ChatModel chatModel,
            StreamingChatModel streamingChatModel,
            Instance<Object> lookup) {
        AiServices<X> builder = AiServices.builder(interfaceClass);
        if (chatModel != null) {
            builder.chatModel(chatModel);
        }
        if (streamingChatModel != null) {
            builder.streamingChatModel(streamingChatModel);
        }
        AgentComponents.resolve(ann, lookup, interfaceClass.getSimpleName()).applyTo(builder);
        return builder.build();
    }

    private static <X> X createForSequence(
            Instance<Object> lookup, Class<X> interfaceClass, RegisterSequenceAgent ann) {
        return buildComposed(
                AgenticServices.sequenceBuilder(interfaceClass),
                ann.name(),
                ann.description(),
                ann.outputKey(),
                ann.subAgentNames(),
                ann.agentListenerName(),
                lookup);
    }

    private static <X> X createForLoop(Instance<Object> lookup, Class<X> interfaceClass, RegisterLoopAgent ann) {
        LoopAgentService<X> loopBuilder = AgenticServices.loopBuilder(interfaceClass);
        loopBuilder.maxIterations(ann.maxIterations());
        applyLoopExitCondition(
                loopBuilder,
                ann.exitConditionName(),
                ann.exitConditionDescription(),
                ann.testAfterEachIteration(),
                lookup);
        return buildComposed(
                loopBuilder,
                ann.name(),
                ann.description(),
                ann.outputKey(),
                ann.subAgentNames(),
                ann.agentListenerName(),
                lookup);
    }

    private static <X> X createForParallel(
            Instance<Object> lookup, Class<X> interfaceClass, RegisterParallelAgent ann) {
        return buildComposed(
                AgenticServices.parallelBuilder(interfaceClass),
                ann.name(),
                ann.description(),
                ann.outputKey(),
                ann.subAgentNames(),
                ann.agentListenerName(),
                lookup);
    }

    private static <X> X createForParallelMapper(
            Instance<Object> lookup, Class<X> interfaceClass, RegisterParallelMapperAgent ann) {
        String itemsKey = CdiLookupHelper.resolveExpression(ann.itemsKey());
        if (!hasText(itemsKey)) {
            throw new IllegalArgumentException(
                    "@RegisterParallelMapperAgent on " + interfaceClass.getSimpleName() + ": 'itemsKey' is required.");
        }
        ParallelMapperService<X> mapperBuilder = AgenticServices.parallelMapperBuilder(interfaceClass);
        mapperBuilder.itemsProvider(itemsKey);
        return buildComposed(
                mapperBuilder,
                ann.name(),
                ann.description(),
                ann.outputKey(),
                ann.subAgentNames(),
                ann.agentListenerName(),
                lookup);
    }

    private static <X> X createForConditional(
            Instance<Object> lookup, Class<X> interfaceClass, RegisterConditionalAgent ann) {
        return buildComposed(
                AgenticServices.conditionalBuilder(interfaceClass),
                ann.name(),
                ann.description(),
                ann.outputKey(),
                ann.subAgentNames(),
                ann.agentListenerName(),
                lookup);
    }

    private static <X> X createForSupervisor(
            Instance<Object> lookup, Class<X> interfaceClass, RegisterSupervisorAgent ann) {
        ChatModel chatModel = CdiLookupHelper.resolveSingle(lookup, ChatModel.class, ann.chatModelName());
        SupervisorAgentService<X> builder = AgenticServices.supervisorBuilder(interfaceClass);
        if (chatModel != null) {
            builder.chatModel(chatModel);
        }
        ChatMemoryProvider chatMemoryProvider =
                CdiLookupHelper.resolveSingle(lookup, ChatMemoryProvider.class, ann.chatMemoryProviderName());
        if (chatMemoryProvider != null) {
            builder.chatMemoryProvider(chatMemoryProvider);
        }
        builder.maxAgentsInvocations(ann.maxAgentsInvocations());
        builder.contextGenerationStrategy(ann.supervisorContextStrategy());
        builder.responseStrategy(ann.supervisorResponseStrategy());
        String supervisorContext = CdiLookupHelper.resolveExpression(ann.supervisorContext());
        if (hasText(supervisorContext)) {
            builder.supervisorContext(supervisorContext);
        }
        List<Object> subAgents = resolveSubAgents(lookup, ann.subAgentNames());
        configureCommonFields(
                builder::subAgents,
                builder::name,
                builder::description,
                builder::outputKey,
                ann.name(),
                ann.description(),
                ann.outputKey(),
                subAgents);
        applyListener(builder::listener, ann.agentListenerName(), lookup);
        return builder.build();
    }

    private static <X> X createForPlanner(Instance<Object> lookup, Class<X> interfaceClass, RegisterPlannerAgent ann) {
        if (!hasText(ann.plannerName()) && !hasPlannerSupplierMethod(interfaceClass)) {
            throw new IllegalArgumentException("@RegisterPlannerAgent on " + interfaceClass.getSimpleName()
                    + ": either 'plannerName' must be set or the interface must declare a static"
                    + " @PlannerSupplier method.");
        }
        PlannerBasedService<X> plannerBuilder = AgenticServices.plannerBuilder(interfaceClass);
        applyPlanner(plannerBuilder, ann.plannerName(), interfaceClass, lookup);
        return buildComposed(
                plannerBuilder,
                ann.name(),
                ann.description(),
                ann.outputKey(),
                ann.subAgentNames(),
                ann.agentListenerName(),
                lookup);
    }

    private static <X> X createForA2A(Instance<Object> lookup, Class<X> interfaceClass, RegisterA2AAgent ann) {
        String url = CdiLookupHelper.resolveExpression(ann.a2aServerUrl());
        if (!hasText(url)) {
            throw new IllegalArgumentException(
                    "@RegisterA2AAgent on " + interfaceClass.getSimpleName() + ": 'a2aServerUrl' is required.");
        }
        return loadA2ABuilder()
                .build(
                        interfaceClass,
                        url,
                        CdiLookupHelper.resolveExpression(ann.outputKey()),
                        ann.async(),
                        ann.agentListenerName(),
                        lookup);
    }

    private static <X> X createForMcpClient(
            Instance<Object> lookup, Class<X> interfaceClass, RegisterMcpClientAgent ann) {
        String clientName = CdiLookupHelper.resolveExpression(ann.mcpClientName());
        if (!hasText(clientName)) {
            throw new IllegalArgumentException(
                    "@RegisterMcpClientAgent on " + interfaceClass.getSimpleName() + ": 'mcpClientName' is required.");
        }
        String toolName = CdiLookupHelper.resolveExpression(ann.mcpToolName());
        if (!hasText(toolName)) {
            throw new IllegalArgumentException(
                    "@RegisterMcpClientAgent on " + interfaceClass.getSimpleName() + ": 'mcpToolName' is required.");
        }
        Instance<Object> clientInstance = lookup.select(Object.class, NamedLiteral.of(clientName));
        if (!clientInstance.isResolvable()) {
            throw new IllegalStateException("@RegisterMcpClientAgent on " + interfaceClass.getSimpleName()
                    + ": MCP client bean '" + clientName + "' is not resolvable.");
        }
        Object mcpClient = clientInstance.get();
        var builder = McpService.get().mcpBuilder(mcpClient, interfaceClass);
        builder.toolName(toolName);
        if (ann.mcpInputKeys().length > 0) {
            builder.inputKeys(ann.mcpInputKeys());
        }
        String outputKey = CdiLookupHelper.resolveExpression(ann.outputKey());
        if (hasText(outputKey)) {
            builder.outputKey(outputKey);
        }
        builder.async(ann.async());
        AgentListener listener = CdiLookupHelper.resolveSingle(lookup, AgentListener.class, ann.agentListenerName());
        if (listener != null) {
            builder.listener(listener);
        }
        return builder.build();
    }

    private static <X> X createForHumanInTheLoop(
            Instance<Object> lookup, Class<X> interfaceClass, RegisterHumanInTheLoopAgent ann) {
        HumanInTheLoop.HumanInTheLoopBuilder builder = AgenticServices.humanInTheLoopBuilder();
        String askUserMethodName = CdiLookupHelper.resolveExpression(ann.askUser());
        Method askUserMethod = findAskUserMethodOnInterface(interfaceClass, askUserMethodName);
        builder.responseProvider(scope -> {
            try {
                return askUserMethod.invoke(null, scope);
            } catch (Exception e) {
                Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null
                        ? ite.getCause()
                        : e;
                if (cause instanceof RuntimeException re) throw re;
                if (cause instanceof Error err) throw err;
                throw new RuntimeException(cause);
            }
        });
        String outputKey = CdiLookupHelper.resolveExpression(ann.outputKey());
        if (hasText(outputKey)) {
            builder.outputKey(outputKey);
        }
        String description = CdiLookupHelper.resolveExpression(ann.description());
        if (hasText(description)) {
            builder.description(description);
        }
        builder.async(ann.async());
        AgentListener listener = CdiLookupHelper.resolveSingle(lookup, AgentListener.class, ann.agentListenerName());
        if (listener != null) {
            builder.listener(listener);
        }
        HumanInTheLoop spec = builder.build();
        Method entryMethod = findEntryMethod(interfaceClass);
        NonAiAgentInstance agentInstance = buildNonAiAgentInstance(
                interfaceClass, entryMethod, ann.name(), description, outputKey, ann.async(), listener);
        InvocationHandler handler = (proxy, method, args) -> {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> AGENT_TOSTRING_PREFIX + agentInstance.name() + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> method.invoke(spec, args);
                };
            }
            if (HumanInTheLoopHolder.class.isAssignableFrom(declaringClass)) {
                return spec;
            }
            if (declaringClass.isAssignableFrom(InternalAgent.class)) {
                return method.invoke(agentInstance, args);
            }
            throw new UnsupportedOperationException(
                    "HUMAN_IN_THE_LOOP agents must be invoked through the agentic system");
        };
        return (X) java.lang.reflect.Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[] {interfaceClass, InternalAgent.class, HumanInTheLoopHolder.class},
                handler);
    }

    // =========================================================================
    // Shared builders
    // =========================================================================

    /**
     * Constructs a {@link NonAiAgentInstance} from resolved annotation values, handling name fall-back to the entry
     * method name (or interface simple name when there is no entry method). Shared by {@link #createForSimple} and
     * {@link #createForHumanInTheLoop}, which both wrap a delegate as a non-AI proxy.
     *
     * @param rawName the raw (possibly expression) name from the annotation
     * @param description already-resolved description (empty string when absent)
     * @param outputKey already-resolved output key (empty string when absent)
     */
    private static NonAiAgentInstance buildNonAiAgentInstance(
            Class<?> interfaceClass,
            Method entryMethod,
            String rawName,
            String description,
            String outputKey,
            boolean async,
            AgentListener listener) {
        String name = CdiLookupHelper.resolveExpression(rawName);
        if (!hasText(name)) {
            name = entryMethod != null ? entryMethod.getName() : interfaceClass.getSimpleName();
        }
        List<AgentArgument> arguments = entryMethod != null ? AgentUtil.argumentsFromMethod(entryMethod) : List.of();
        return new NonAiAgentInstance(
                interfaceClass,
                name,
                description,
                entryMethod != null ? entryMethod.getGenericReturnType() : String.class,
                outputKey,
                async,
                arguments,
                listener);
    }

    @SuppressWarnings("unchecked")
    private static <X, S extends AgenticService<S, X>> X buildComposed(
            S builder,
            String name,
            String description,
            String outputKey,
            String[] subAgentNames,
            String agentListenerName,
            Instance<Object> lookup) {
        List<Object> subAgents = resolveSubAgents(lookup, subAgentNames);
        configureCommonFields(
                builder::subAgents,
                builder::name,
                builder::description,
                builder::outputKey,
                name,
                description,
                outputKey,
                subAgents);
        applyListener(builder::listener, agentListenerName, lookup);
        return builder.build();
    }

    private static void configureCommonFields(
            Consumer<Collection<?>> subAgentsSetter,
            Consumer<String> nameSetter,
            Consumer<String> descriptionSetter,
            Consumer<String> outputKeySetter,
            String rawName,
            String rawDescription,
            String rawOutputKey,
            List<Object> subAgents) {
        if (!subAgents.isEmpty()) {
            subAgentsSetter.accept(subAgents);
        }
        String name = CdiLookupHelper.resolveExpression(rawName);
        if (hasText(name)) {
            nameSetter.accept(name);
        }
        String description = CdiLookupHelper.resolveExpression(rawDescription);
        if (hasText(description)) {
            descriptionSetter.accept(description);
        }
        String outputKey = CdiLookupHelper.resolveExpression(rawOutputKey);
        if (hasText(outputKey)) {
            outputKeySetter.accept(outputKey);
        }
    }

    private static void applyListener(
            Consumer<AgentListener> setter, String agentListenerName, Instance<Object> lookup) {
        AgentListener listener = CdiLookupHelper.resolveSingle(lookup, AgentListener.class, agentListenerName);
        if (listener != null) {
            setter.accept(listener);
        }
    }

    // =========================================================================
    // AgentComponents — resolves dependencies for @RegisterSimpleAgent
    // =========================================================================

    private record AgentComponents(
            ToolProvider toolProvider,
            List<Object> tools,
            ChatMemory chatMemory,
            ChatMemoryProvider chatMemoryProvider,
            RetrievalAugmentor retrievalAugmentor,
            ContentRetriever contentRetriever,
            List<InputGuardrail> inputGuardrails,
            List<OutputGuardrail> outputGuardrails) {

        static AgentComponents resolve(RegisterSimpleAgent ann, Instance<Object> lookup, String interfaceName) {
            ToolProvider toolProvider =
                    CdiLookupHelper.resolveSingle(lookup, ToolProvider.class, ann.toolProviderName());
            List<Object> tools = new java.util.ArrayList<>();
            if (ann.tools().length > 0) {
                tools.addAll(CdiLookupHelper.resolveToolInstances(ann.tools(), lookup));
            }
            if (ann.toolNames().length > 0) {
                tools.addAll(CdiLookupHelper.resolveToolsByName(ann.toolNames(), lookup));
            }
            if (toolProvider != null && !tools.isEmpty()) {
                LOGGER.warning("Both toolProviderName and tools/toolNames[] are configured on "
                        + interfaceName
                        + "; overlapping tool names will cause IllegalConfigurationException at runtime.");
            }
            ChatMemory chatMemory = CdiLookupHelper.resolveSingle(lookup, ChatMemory.class, ann.chatMemoryName());
            ChatMemoryProvider chatMemoryProvider =
                    CdiLookupHelper.resolveSingle(lookup, ChatMemoryProvider.class, ann.chatMemoryProviderName());
            RetrievalAugmentor retrievalAugmentor =
                    CdiLookupHelper.resolveSingle(lookup, RetrievalAugmentor.class, ann.retrievalAugmentorName());
            ContentRetriever contentRetriever = retrievalAugmentor == null
                    ? CdiLookupHelper.resolveSingle(lookup, ContentRetriever.class, ann.contentRetrieverName())
                    : null;
            List<InputGuardrail> inputGuardrails = CdiLookupHelper.resolveInputGuardrails(
                    lookup, ann.inputGuardrails(), ann.inputGuardrailNames(), interfaceName);
            List<OutputGuardrail> outputGuardrails = CdiLookupHelper.resolveOutputGuardrails(
                    lookup, ann.outputGuardrails(), ann.outputGuardrailNames(), interfaceName);
            return new AgentComponents(
                    toolProvider,
                    tools,
                    chatMemory,
                    chatMemoryProvider,
                    retrievalAugmentor,
                    contentRetriever,
                    inputGuardrails,
                    outputGuardrails);
        }

        void applyTo(AiServices<?> builder) {
            if (toolProvider != null) {
                builder.toolProvider(toolProvider);
            }
            if (!tools.isEmpty()) {
                builder.tools(tools);
            }
            if (chatMemory != null) {
                builder.chatMemory(chatMemory);
            }
            if (chatMemoryProvider != null) {
                builder.chatMemoryProvider(chatMemoryProvider);
            }
            if (retrievalAugmentor != null) {
                builder.retrievalAugmentor(retrievalAugmentor);
            } else if (contentRetriever != null) {
                builder.contentRetriever(contentRetriever);
            }
            if (!inputGuardrails.isEmpty()) {
                builder.inputGuardrails(inputGuardrails);
            }
            if (!outputGuardrails.isEmpty()) {
                builder.outputGuardrails(outputGuardrails);
            }
        }

        @SuppressWarnings("unchecked")
        void applyTo(AgentBuilder<?, ?> builder) {
            if (toolProvider != null) {
                builder.toolProvider(toolProvider);
            }
            if (!tools.isEmpty()) {
                builder.tools(tools.toArray(new Object[0]));
            }
            if (chatMemory != null) {
                builder.chatMemory(chatMemory);
            }
            if (chatMemoryProvider != null) {
                builder.chatMemoryProvider(chatMemoryProvider);
            }
            if (retrievalAugmentor != null) {
                builder.retrievalAugmentor(retrievalAugmentor);
            } else if (contentRetriever != null) {
                builder.contentRetriever(contentRetriever);
            }
            if (!inputGuardrails.isEmpty()) {
                builder.inputGuardrails(inputGuardrails.toArray(InputGuardrail[]::new));
            }
            if (!outputGuardrails.isEmpty()) {
                builder.outputGuardrails(outputGuardrails.toArray(OutputGuardrail[]::new));
            }
        }
    }

    // =========================================================================
    // Planner, loop exit-condition, sub-agent helpers
    // =========================================================================

    private static <X> void applyPlanner(
            PlannerBasedService<X> plannerBuilder,
            String rawPlannerName,
            Class<?> interfaceClass,
            Instance<Object> lookup) {
        String name = CdiLookupHelper.resolveExpression(rawPlannerName);
        if (!hasText(name)) {
            return;
        }
        Planner planner = CdiLookupHelper.resolveSingle(lookup, Planner.class, name);
        if (planner == null) {
            LOGGER.log(Level.WARNING, "Planner bean ''{0}'' is not resolvable, skipping", name);
            return;
        }
        plannerBuilder.planner(() -> planner);
    }

    @SuppressWarnings("unchecked")
    private static <X> void applyLoopExitCondition(
            LoopAgentService<X> loopBuilder,
            String rawExitConditionName,
            String rawExitConditionDescription,
            boolean testAfterEachIteration,
            Instance<Object> lookup) {
        String name = CdiLookupHelper.resolveExpression(rawExitConditionName);
        if (!hasText(name)) {
            return;
        }
        Object bean = resolveParameterizedBean(lookup, name, Predicate.class, BiPredicate.class);
        if (bean == null) {
            LOGGER.log(
                    Level.WARNING,
                    "Exit condition ''{0}'' is not resolvable as Predicate or BiPredicate, skipping",
                    name);
            return;
        }
        String description = CdiLookupHelper.resolveExpression(rawExitConditionDescription);
        if (bean instanceof BiPredicate) {
            BiPredicate<AgenticScope, Integer> typed = (BiPredicate<AgenticScope, Integer>) bean;
            if (hasText(description)) {
                loopBuilder.exitCondition(description, typed);
            } else {
                loopBuilder.exitCondition(typed);
            }
        } else {
            Predicate<AgenticScope> typed = (Predicate<AgenticScope>) bean;
            if (hasText(description)) {
                loopBuilder.exitCondition(description, typed);
            } else {
                loopBuilder.exitCondition(typed);
            }
        }
        if (testAfterEachIteration) {
            loopBuilder.testExitAtLoopEnd(true);
        }
    }

    private static List<Object> resolveSubAgents(Instance<Object> lookup, String[] subAgentNames) {
        List<Object> subAgents = new ArrayList<>();
        for (String name : subAgentNames) {
            name = CdiLookupHelper.resolveExpression(name);
            if (!hasText(name)) {
                continue;
            }
            Instance<Object> beanInstance = lookup.select(Object.class, NamedLiteral.of(name));
            if (beanInstance.isResolvable()) {
                subAgents.add(toAgentExecutor(beanInstance.get(), name));
            } else {
                LOGGER.log(Level.WARNING, "Sub-agent ''{0}'' is not resolvable, skipping", name);
            }
        }
        return subAgents;
    }

    /** Marker interface allowing {@link #toAgentExecutor} to retrieve the underlying {@link HumanInTheLoop} spec. */
    public interface HumanInTheLoopHolder {
        HumanInTheLoop getHumanInTheLoop();
    }

    // =========================================================================
    // toAgentExecutor — public utility
    // =========================================================================

    /**
     * Wraps a CDI-managed agent bean into an {@link AgentExecutor} suitable for use with agentic builders. CDI client
     * proxies (e.g., Weld proxies for {@code @ApplicationScoped} beans) lose method annotations, which prevents
     * {@link AgentUtil#agentToExecutor(Object)} from finding the agent's entry point. This method inspects the original
     * interface instead.
     *
     * <p>Use this when passing CDI-injected agent beans directly to programmatic agentic builders:
     *
     * <pre>{@code
     * AgenticServices.loopBuilder()
     *     .subAgents(CommonAgentCreator.toAgentExecutor(scorer), editor)
     *     .build();
     * }</pre>
     */
    public static Object toAgentExecutor(Object bean) {
        return toAgentExecutor(bean, null);
    }

    /**
     * Like {@link #toAgentExecutor(Object)} but also accepts the CDI bean name so that the BeanManager can be used as a
     * fallback when Weld's proxy class hierarchy does not expose the original agent interface. This fallback is
     * necessary on WildFly/Weld where the scope proxy class for an {@code @ApplicationScoped} synthetic bean may not
     * directly implement the user's annotated interface in its {@code getInterfaces()} chain.
     */
    private static Object toAgentExecutor(Object bean, String beanName) {
        if (bean instanceof AgentExecutor) {
            return bean;
        }
        if (bean instanceof InternalAgent ia) {
            List<Class<?>> hierarchy = allInterfaces(bean.getClass());
            LOGGER.log(
                    Level.FINE,
                    "toAgentExecutor: bean class={0}, beanName={1}, interfaces in hierarchy={2}",
                    new Object[] {
                        bean.getClass().getName(),
                        beanName,
                        hierarchy.stream().map(Class::getName).collect(Collectors.joining(", "))
                    });
            // Primary path: traverse the full class hierarchy (covers @Dependent beans and most CDI impls)
            for (Class<?> iface : hierarchy) {
                AgentExecutor executor = tryBuildExecutor(bean, ia, iface);
                if (executor != null) {
                    LOGGER.log(Level.FINE, "toAgentExecutor: resolved via class hierarchy using {0}", iface.getName());
                    return executor;
                }
            }
            // Fallback: use BeanManager to find the original interface from the bean's declared types.
            // Needed on WildFly/Weld where the scope proxy class hides the user interface.
            if (beanName != null) {
                Class<?> agentIface = findAgentInterfaceViaBeanManager(beanName);
                LOGGER.log(
                        Level.FINE,
                        "toAgentExecutor: class hierarchy failed, BeanManager lookup for ''{0}'' returned {1}",
                        new Object[] {beanName, agentIface != null ? agentIface.getName() : "null"});
                if (agentIface != null) {
                    AgentExecutor executor = tryBuildExecutor(bean, ia, agentIface);
                    if (executor != null) return executor;
                }
            }
            // Last resort: unwrap the CDI scope proxy to let AgentUtil inspect the actual instance.
            // Weld's scope proxies implement TargetInstanceProxy.getTargetInstance() which returns the
            // real bean. For marker-interface agents (no abstract method), the agentic framework stores
            // the executor on the actual instance and AgentUtil needs to see it directly.
            InternalAgent target = unwrapCdiProxy(ia);
            LOGGER.log(
                    Level.FINE,
                    "toAgentExecutor: all strategies failed for bean class={0},"
                            + " delegating to AgentUtil on unwrapped target={1}",
                    new Object[] {bean.getClass().getName(), target.getClass().getName()});
            return AgentUtil.agentToExecutor(target);
        }
        return AgentUtil.agentToExecutor(bean);
    }

    /**
     * Attempts to unwrap a CDI scope proxy to its underlying target instance. Weld's scope proxies implement
     * {@code TargetInstanceProxy.getTargetInstance()}; other containers may expose a similar mechanism. Returns
     * {@code ia} unchanged when no unwrapping is possible.
     */
    private static InternalAgent unwrapCdiProxy(InternalAgent ia) {
        try {
            Method getTargetInstance = ia.getClass().getMethod("getTargetInstance");
            Object target = getTargetInstance.invoke(ia);
            if (target instanceof InternalAgent unwrapped && target != ia) {
                return unwrapped;
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.FINE,
                    "unwrapCdiProxy: could not unwrap CDI proxy of type "
                            + ia.getClass().getName(),
                    e);
        }
        return ia;
    }

    private static AgentExecutor tryBuildExecutor(Object bean, InternalAgent ia, Class<?> iface) {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(iface);
        if (meta == null) return null;
        Method entryMethod = findEntryMethod(iface);
        // HITL agents may be marker interfaces with no abstract method (only static methods).
        // Even when an entry method exists, execution must go through HumanInTheLoop.askUser
        // on the spec rather than through the user's interface method on the CDI proxy.
        if (bean instanceof HumanInTheLoopHolder holder) {
            HumanInTheLoop spec = holder.getHumanInTheLoop();
            String resolvedName = meta.name();
            String name = hasText(resolvedName)
                    ? resolvedName
                    : (entryMethod != null ? entryMethod.getName() : iface.getSimpleName());
            Method askUser = findAskUserMethod();
            AgentInvoker invoker = AgentInvoker.fromSpec(spec, askUser, name);
            return new AgentExecutor(invoker, spec);
        }
        if (entryMethod == null) {
            LOGGER.log(
                    Level.WARNING,
                    "tryBuildExecutor: {0} is an agent interface but findEntryMethod returned null"
                            + " (no abstract method declared directly on the interface)",
                    iface.getName());
            return null;
        }
        String resolvedName = meta.name();
        String name = hasText(resolvedName) ? resolvedName : entryMethod.getName();
        String desc = meta.description();
        String outputKey = meta.outputKey();
        boolean async = meta.async();
        AgentInvoker invoker = AgentUtil.nonAiAgentInvoker(entryMethod, name, desc, outputKey, async);
        return new AgentExecutor(invoker, ia);
    }

    private static Class<?> findAgentInterfaceViaBeanManager(String beanName) {
        try {
            BeanManager bm = CDI.current().getBeanManager();
            Set<Bean<?>> beans = bm.getBeans(beanName);
            LOGGER.log(
                    Level.FINE,
                    "findAgentInterfaceViaBeanManager: BeanManager.getBeans(''{0}'') returned {1} bean(s)",
                    new Object[] {beanName, beans.size()});
            for (Bean<?> b : beans) {
                LOGGER.log(Level.FINE, "findAgentInterfaceViaBeanManager: bean={0}, types={1}", new Object[] {
                    b.getBeanClass().getName(),
                    b.getTypes().stream().map(Object::toString).collect(Collectors.joining(", "))
                });
                for (Type t : b.getTypes()) {
                    if (t instanceof Class<?> cls && AgentAnnotationMeta.isAgentInterface(cls)) {
                        return cls;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "findAgentInterfaceViaBeanManager: BeanManager lookup failed for ''" + beanName + "''",
                    e);
        }
        return null;
    }

    private static boolean hasPlannerSupplierMethod(Class<?> interfaceClass) {
        for (Method method : interfaceClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PlannerSupplier.class)) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // CDI resolution helpers
    // =========================================================================

    /**
     * Tries to resolve a CDI bean by name whose declared type is parameterized (e.g. {@code Predicate<AgenticScope>}).
     * Some containers (notably Weld on WildFly) do not match a raw type qualifier when the bean's actual generic type
     * differs. The resolution strategy is:
     *
     * <ol>
     *   <li>For each {@code candidateType}, attempt {@link CdiLookupHelper#resolveSingle} by raw type and name.
     *   <li>If all raw lookups fail, resolve by name as {@code Object} and return the first candidate that is an
     *       {@code instanceof} one of the types.
     * </ol>
     *
     * @return the resolved bean, or {@code null} when no resolvable bean matches any candidate type.
     */
    @SuppressWarnings("unchecked")
    private static Object resolveParameterizedBean(Instance<Object> lookup, String name, Class<?>... candidateTypes) {
        for (Class<?> type : candidateTypes) {
            Object bean = CdiLookupHelper.resolveSingle(lookup, (Class<Object>) type, name);
            if (bean != null) return bean;
        }
        Instance<Object> namedInst = lookup.select(Object.class, NamedLiteral.of(name));
        if (namedInst.isResolvable()) {
            Object candidate = namedInst.get();
            for (Class<?> type : candidateTypes) {
                if (type.isInstance(candidate)) return candidate;
            }
        }
        return null;
    }

    // =========================================================================
    // A2A SPI loader
    // =========================================================================

    private static final class A2ABuilderHolder {
        static final dev.langchain4j.cdi.agent.spi.A2AAgentBuilder INSTANCE = ServiceLoader.load(
                        dev.langchain4j.cdi.agent.spi.A2AAgentBuilder.class)
                .findFirst()
                .orElse(null);
    }

    private static dev.langchain4j.cdi.agent.spi.A2AAgentBuilder loadA2ABuilder() {
        if (A2ABuilderHolder.INSTANCE == null) {
            throw new IllegalStateException(
                    "A2A topology requires the langchain4j-cdi-a2a dependency on the classpath. "
                            + "Add dev.langchain4j.cdi:langchain4j-cdi-a2a to your project.");
        }
        return A2ABuilderHolder.INSTANCE;
    }

    // =========================================================================
    // Entry-method discovery
    // =========================================================================

    /**
     * Collects all interfaces implemented by {@code clazz} and its superclasses. Necessary because CDI client proxies
     * (e.g., Weld proxies in WildFly) may extend an intermediate subclass that implements the user interface, so
     * {@code clazz.getInterfaces()} alone does not always include the original annotated interface.
     */
    private static List<Class<?>> allInterfaces(Class<?> clazz) {
        Set<Class<?>> seen = new LinkedHashSet<>();
        collectInterfaces(clazz, seen);
        return new ArrayList<>(seen);
    }

    private static void collectInterfaces(Class<?> clazz, Set<Class<?>> seen) {
        if (clazz == null || clazz == Object.class) return;
        for (Class<?> iface : clazz.getInterfaces()) {
            if (seen.add(iface)) {
                collectInterfaces(iface, seen);
            }
        }
        collectInterfaces(clazz.getSuperclass(), seen);
    }

    private static Method findAskUserMethodOnInterface(Class<?> interfaceClass, String askUserMethodName) {
        Predicate<Method> signature = m -> Modifier.isStatic(m.getModifiers())
                && m.getParameterCount() == 1
                && AgenticScope.class.isAssignableFrom(m.getParameterTypes()[0])
                && String.class.equals(m.getReturnType());
        if (hasText(askUserMethodName)) {
            return Arrays.stream(interfaceClass.getDeclaredMethods())
                    .filter(signature)
                    .filter(m -> m.getName().equals(askUserMethodName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "@RegisterHumanInTheLoopAgent interface " + interfaceClass.getName()
                                    + " declares askUser=\"" + askUserMethodName
                                    + "\" but no static String " + askUserMethodName
                                    + "(AgenticScope) method was found"));
        }
        List<Method> candidates = Arrays.stream(interfaceClass.getDeclaredMethods())
                .filter(signature)
                .toList();
        if (candidates.size() > 1) {
            throw new IllegalArgumentException("@RegisterHumanInTheLoopAgent interface " + interfaceClass.getName()
                    + " declares multiple static String(AgenticScope) methods ("
                    + candidates.stream().map(Method::getName).collect(Collectors.joining(", "))
                    + "); set the askUser attribute to select one");
        }
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("@RegisterHumanInTheLoopAgent interface " + interfaceClass.getName()
                    + " must declare a static String(AgenticScope) method to provide the human response");
        }
        return candidates.get(0);
    }

    private static Method findAskUserMethod() {
        return AgentUtil.getAnnotatedMethodOnClass(HumanInTheLoop.class, Agent.class)
                .orElseGet(() -> {
                    try {
                        return HumanInTheLoop.class.getMethod("askUser", AgenticScope.class);
                    } catch (NoSuchMethodException e) {
                        throw new IllegalStateException("HumanInTheLoop.askUser(AgenticScope) not found", e);
                    }
                });
    }

    private static Method findEntryMethod(Class<?> agentInterface) {
        // Check methods declared directly on the interface first.
        List<Method> declared = Arrays.stream(agentInterface.getDeclaredMethods())
                .filter(m -> !m.isDefault() && !Modifier.isStatic(m.getModifiers()))
                .toList();
        if (declared.size() > 1) {
            throw new IllegalArgumentException("Agent interface "
                    + agentInterface.getName()
                    + " must declare exactly one abstract method, but found: "
                    + declared.stream().map(Method::getName).collect(Collectors.joining(", ")));
        }
        if (!declared.isEmpty()) {
            return declared.get(0);
        }
        // Fall back to searching all public abstract methods including those inherited from parent
        // interfaces. This handles the pattern where the entry method is declared in a shared base
        // interface extended by the agent interface.
        List<Method> inherited = Arrays.stream(agentInterface.getMethods())
                .filter(m -> !m.isDefault() && !m.isSynthetic() && !Modifier.isStatic(m.getModifiers()))
                .distinct()
                .toList();
        if (inherited.size() > 1) {
            LOGGER.log(
                    Level.WARNING,
                    "Agent interface {0} inherits multiple abstract methods ({1}); cannot determine"
                            + " entry point — declare exactly one abstract method on the interface itself",
                    new Object[] {
                        agentInterface.getName(),
                        inherited.stream().map(Method::getName).collect(Collectors.joining(", "))
                    });
            return null;
        }
        return inherited.isEmpty() ? null : inherited.get(0);
    }
}
