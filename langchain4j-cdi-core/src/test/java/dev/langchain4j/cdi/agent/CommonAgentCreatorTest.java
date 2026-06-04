package dev.langchain4j.cdi.agent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.internal.InternalAgent;
import dev.langchain4j.agentic.internal.McpClientBuilder;
import dev.langchain4j.agentic.internal.McpService;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.cdi.agent.CommonAgentCreator.HumanInTheLoopHolder;
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
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.V;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class CommonAgentCreatorTest {

    // --- Tool classes ---
    static class ToolAImpl {

        public ToolAImpl() {}

        @Tool
        public String ping() {
            return "pong";
        }
    }

    // --- Guardrail classes ---
    public static class TestInputGuardrail implements InputGuardrail {

        @Override
        public InputGuardrailResult validate(UserMessage userMessage) {
            return success();
        }
    }

    public static class TestOutputGuardrail implements OutputGuardrail {

        @Override
        public OutputGuardrailResult validate(AiMessage responseFromLLM) {
            return success();
        }
    }

    // --- Test interfaces for SIMPLE topology (non-@Agent path) ---
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(chatModelName = "#default")
    interface SimpleAgent {

        String chat(@V("question") String question);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(name = "myAgent", chatModelName = "#default")
    interface NamedAgent {

        String chat(@V("question") String question);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(chatModelName = "#default")
    interface AgentWithDoWork {

        String doWork(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(
            chatModelName = "#default",
            chatMemoryName = "mem1",
            retrievalAugmentorName = "ra1",
            contentRetrieverName = "cr1")
    interface AgentWithAllDeps {

        String chat(@V("question") String question);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(chatModelName = "#default", outputKey = "myKey")
    interface AgentWithOutputKey {

        String chat(@V("question") String question);
    }

    // --- Test interface for SIMPLE topology (@Agent path) ---
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(chatModelName = "#default")
    interface AgenticAgent {

        @Agent(description = "test agent")
        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(chatModelName = "#default", outputKey = "agentOutput")
    interface AgenticAgentWithOutputKey {

        @Agent(description = "test agent")
        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(
            chatModelName = "#default",
            name = "myAgenticAgent",
            description = "an agentic agent",
            outputKey = "myOutput",
            async = true)
    interface AgenticAgentWithMetadata {

        @Agent(description = "test agent")
        String process(@V("input") String input);
    }

    // --- Tool resolution interfaces ---
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(toolProviderName = "tp1", chatModelName = "#default")
    interface AgentWithToolProvider {

        String chat(@V("question") String question);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(
            chatModelName = "#default",
            toolNames = {"myTool"})
    interface AgentWithToolNames {

        String chat(@V("question") String question);
    }

    // --- Guardrail interfaces ---
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(
            chatModelName = "#default",
            inputGuardrailNames = {"myIG"},
            outputGuardrailNames = {"myOG"})
    interface AgentWithNamedGuardrails {

        String chat(@V("question") String question);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(
            chatModelName = "#default",
            inputGuardrailNames = {"nonExistent"})
    interface AgentWithUnresolvableNamedGuardrail {

        String chat(@V("question") String question);
    }

    // --- Expression-resolved interfaces ---
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(chatModelName = "${#default}")
    interface ExpressionChatModelAgent {

        String chat(@V("q") String q);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSimpleAgent(name = "${myAgent}", chatModelName = "#default")
    interface ExpressionNamedAgent {

        String chat(@V("q") String q);
    }

    // --- A2A interfaces ---
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterA2AAgent(a2aServerUrl = "")
    interface A2AAgentBlankUrl {

        String chat(String question);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterA2AAgent(a2aServerUrl = "http://remote-agent:8080")
    interface A2AAgentWithUrl {

        String chat(String question);
    }

    // --- Test interfaces for composed topologies ---
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSequenceAgent(subAgentNames = {"stepA", "stepB"})
    interface SequenceOrchestrator {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterLoopAgent(
            subAgentNames = {"worker"},
            maxIterations = 3)
    interface LoopOrchestrator {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterLoopAgent(
            subAgentNames = {"worker"},
            maxIterations = 3,
            exitConditionName = "myExitCondition")
    interface LoopOrchestratorWithExitCondition {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterLoopAgent(
            subAgentNames = {"worker"},
            maxIterations = 3,
            exitConditionName = "myExitCondition",
            exitConditionDescription = "Check if done")
    interface LoopOrchestratorWithExitConditionAndDescription {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterLoopAgent(
            subAgentNames = {"worker"},
            maxIterations = 5,
            exitConditionName = "myBiExitCondition")
    interface LoopOrchestratorWithBiPredicateExitCondition {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterLoopAgent(
            subAgentNames = {"worker"},
            maxIterations = 5,
            exitConditionName = "myBiExitCondition",
            exitConditionDescription = "Check if done")
    interface LoopOrchestratorWithBiPredicateAndDescription {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterLoopAgent(
            subAgentNames = {"worker"},
            exitConditionName = "myExitCondition",
            testAfterEachIteration = true)
    interface LoopOrchestratorWithTestAfterEachIteration {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterLoopAgent(
            subAgentNames = {"worker"},
            exitConditionName = "unresolvableCondition")
    interface LoopOrchestratorWithUnresolvableExitCondition {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterParallelAgent(subAgentNames = {"taskA", "taskB"})
    interface ParallelOrchestrator {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterConditionalAgent(subAgentNames = {"branchA", "branchB"})
    interface ConditionalOrchestrator {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSupervisorAgent(
            chatModelName = "#default",
            subAgentNames = {"workerAgent"},
            maxAgentsInvocations = 5)
    interface SupervisorOrchestrator {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterPlannerAgent
    interface PlannerOrchestratorWithoutSupplier {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterPlannerAgent(
            subAgentNames = {"plannerWorker"},
            plannerName = "myPlanner")
    interface PlannerOrchestratorWithPlannerName {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterSupervisorAgent(
            chatModelName = "#default",
            subAgentNames = {"workerAgent"},
            supervisorContext = "Always respond in formal English")
    interface SupervisorOrchestratorWithContext {

        String process(@V("input") String input);
    }

    // --- Test interfaces for PARALLEL_MAPPER topology ---
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterParallelMapperAgent(
            subAgentNames = {"worker"},
            itemsKey = "items")
    interface ParallelMapperOrchestrator {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterParallelMapperAgent(
            subAgentNames = {"worker"},
            itemsKey = "")
    interface ParallelMapperOrchestratorNoItemsKey {

        String process(@V("input") String input);
    }

    // --- Test interfaces for HUMAN_IN_THE_LOOP topology ---
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterHumanInTheLoopAgent
    interface HumanInTheLoopAgent {

        static String askUser(AgenticScope scope) {
            return "approved";
        }

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterHumanInTheLoopAgent(outputKey = "result", description = "Waits for human approval")
    interface HumanInTheLoopAgentNoProvider {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterHumanInTheLoopAgent(
            name = "human-approval-agent",
            description = "Asks human for approval",
            outputKey = "approvalDecision")
    interface HumanInTheLoopMarkerAgent {

        static String askUser(AgenticScope scope) {
            return "approved-by-marker";
        }
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterHumanInTheLoopAgent(askUser = "promptHuman")
    interface HumanInTheLoopNamedMethod {

        static String promptHuman(AgenticScope scope) {
            return "named-response";
        }

        static String otherHelper(AgenticScope scope) {
            return "other";
        }

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterHumanInTheLoopAgent(askUser = "nonExistent")
    interface HumanInTheLoopBadName {

        static String askUser(AgenticScope scope) {
            return "won't match";
        }

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterHumanInTheLoopAgent
    interface HumanInTheLoopAmbiguous {

        static String methodA(AgenticScope scope) {
            return "a";
        }

        static String methodB(AgenticScope scope) {
            return "b";
        }

        String process(@V("input") String input);
    }

    // --- Test interfaces for MCP_CLIENT topology ---
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterMcpClientAgent(mcpClientName = "", mcpToolName = "web_search")
    interface McpClientAgentBlankClientName {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterMcpClientAgent(mcpClientName = "myMcpClient", mcpToolName = "")
    interface McpClientAgentBlankToolName {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterMcpClientAgent(mcpClientName = "unknownClient", mcpToolName = "search")
    interface McpClientAgentUnresolvableClient {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterMcpClientAgent(
            mcpClientName = "myMcpClient",
            mcpToolName = "web_search",
            mcpInputKeys = {"query", "locale"})
    interface McpClientAgentWithInputKeys {

        String process(@V("input") String input);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterMcpClientAgent(mcpClientName = "myMcpClient", mcpToolName = "web_search", async = true)
    interface McpClientAgentAsync {

        String process(@V("input") String input);
    }

    // --- Missing annotation ---
    interface PlainInterface {

        String chat(String question);
    }

    // --- findEntryMethod test interfaces ---
    interface SingleMethodInterface {

        String doSomething();
    }

    interface MultiMethodInterface {

        String first();

        String second();
    }

    interface OnlyDefaultMethodsInterface {

        default String hello() {
            return "hello";
        }
    }

    interface MixedMethodsInterface {

        default String greeting() {
            return "hi";
        }

        String process();
    }

    // =========================================================================
    // Test helper
    // =========================================================================
    @SuppressWarnings("unchecked")
    private static Instance<Object> prepareLookups() {
        Instance<Object> lookup = mock(Instance.class);

        Instance<ChatModel> cm = mock(Instance.class);
        Instance<StreamingChatModel> scm = mock(Instance.class);
        Instance<ContentRetriever> cr = mock(Instance.class);
        Instance<RetrievalAugmentor> ra = mock(Instance.class);
        Instance<ToolProvider> tp = mock(Instance.class);
        Instance<ChatMemory> mem = mock(Instance.class);
        Instance<ChatMemoryProvider> cmp = mock(Instance.class);
        Instance<AgentListener> al = mock(Instance.class);

        ChatModel cmBean = mock(ChatModel.class);
        ContentRetriever crBean = mock(ContentRetriever.class);
        RetrievalAugmentor raBean = mock(RetrievalAugmentor.class);
        ChatMemory memBean = mock(ChatMemory.class);
        ChatMemoryProvider cmpBean = mock(ChatMemoryProvider.class);

        when(lookup.select(ChatModel.class)).thenReturn(cm);
        when(lookup.select(ContentRetriever.class, NamedLiteral.of("cr1"))).thenReturn(cr);
        when(lookup.select(RetrievalAugmentor.class, NamedLiteral.of("ra1"))).thenReturn(ra);
        when(lookup.select(ChatMemory.class, NamedLiteral.of("mem1"))).thenReturn(mem);
        when(lookup.select(ChatMemoryProvider.class, NamedLiteral.of("cmp1"))).thenReturn(cmp);

        when(cm.isResolvable()).thenReturn(true);
        when(scm.isResolvable()).thenReturn(false);
        when(cr.isResolvable()).thenReturn(true);
        when(ra.isResolvable()).thenReturn(true);
        when(tp.isResolvable()).thenReturn(false);
        when(mem.isResolvable()).thenReturn(true);
        when(cmp.isResolvable()).thenReturn(true);
        when(al.isResolvable()).thenReturn(false);

        when(cm.get()).thenReturn(cmBean);
        when(cr.get()).thenReturn(crBean);
        when(ra.get()).thenReturn(raBean);
        when(mem.get()).thenReturn(memBean);
        when(cmp.get()).thenReturn(cmpBean);

        return lookup;
    }

    /**
     * Extends {@link #prepareLookups()} with CDI entries for named sub-agents. Each named bean resolves to a real
     * {@link SimpleAgent} proxy so that {@link CommonAgentCreator#toAgentExecutor} can wrap it in a real
     * {@link AgentExecutor} with a real invoker — as would happen at runtime.
     */
    @SuppressWarnings("unchecked")
    private static Instance<Object> prepareLookupsWithSubAgents(String... agentNames) {
        Instance<Object> lookup = prepareLookups();
        SimpleAgent subAgentProxy = CommonAgentCreator.create(lookup, SimpleAgent.class);
        for (String name : agentNames) {
            Instance<Object> beanInstance = mock(Instance.class);
            when(lookup.select(Object.class, NamedLiteral.of(name))).thenReturn(beanInstance);
            when(beanInstance.isResolvable()).thenReturn(true);
            when(beanInstance.get()).thenReturn(subAgentProxy);
        }
        return lookup;
    }

    // =========================================================================
    // SIMPLE topology -- non-@Agent path
    // =========================================================================
    @Test
    void create_simpleTopology_noAgentAnnotation_buildsNonAiProxy() {
        Instance<Object> lookup = prepareLookups();
        SimpleAgent agent = CommonAgentCreator.create(lookup, SimpleAgent.class);

        assertNotNull(agent);
        assertTrue(agent.toString().contains("Agent["));
        assertInstanceOf(InternalAgent.class, agent);
    }

    @Test
    void create_simpleTopology_usesAnnotationNameInToString() {
        Instance<Object> lookup = prepareLookups();
        NamedAgent agent = CommonAgentCreator.create(lookup, NamedAgent.class);

        assertNotNull(agent);
        assertTrue(agent.toString().contains("Agent[myAgent]"));
    }

    @Test
    void create_simpleTopology_fallsBackToMethodNameWhenNameBlank() {
        Instance<Object> lookup = prepareLookups();
        AgentWithDoWork agent = CommonAgentCreator.create(lookup, AgentWithDoWork.class);

        assertNotNull(agent);
        assertTrue(agent.toString().contains("Agent[doWork]"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_simpleTopology_wiresAllResolvableDependencies() {
        Instance<Object> lookup = prepareLookups();
        AgentWithAllDeps agent = CommonAgentCreator.create(lookup, AgentWithAllDeps.class);

        assertNotNull(agent);
        assertInstanceOf(InternalAgent.class, agent);
    }

    @Test
    void create_simpleTopology_noAgentAnnotation_propagatesOutputKey() {
        Instance<Object> lookup = prepareLookups();
        AgentWithOutputKey agent = CommonAgentCreator.create(lookup, AgentWithOutputKey.class);

        assertNotNull(agent);
        assertInstanceOf(InternalAgent.class, agent);
        assertEquals("myKey", ((InternalAgent) agent).outputKey());
    }

    // =========================================================================
    // SIMPLE topology -- @Agent path
    // =========================================================================
    @Test
    void create_simpleTopology_withAgentAnnotation_buildsAgenticProxy() {
        Instance<Object> lookup = prepareLookups();
        AgenticAgent agent = CommonAgentCreator.create(lookup, AgenticAgent.class);

        assertNotNull(agent);
        assertInstanceOf(InternalAgent.class, agent);
    }

    @Test
    void create_simpleTopology_withAgentAnnotation_propagatesOutputKey() {
        Instance<Object> lookup = prepareLookups();
        AgenticAgentWithOutputKey agent = CommonAgentCreator.create(lookup, AgenticAgentWithOutputKey.class);

        assertNotNull(agent);
        assertInstanceOf(InternalAgent.class, agent);
        assertEquals("agentOutput", ((InternalAgent) agent).outputKey());
    }

    @Test
    void create_simpleTopology_withAgentAnnotation_honorsOutputKeyNameDescriptionAsync() {
        Instance<Object> lookup = prepareLookups();
        AgenticAgentWithMetadata agent = CommonAgentCreator.create(lookup, AgenticAgentWithMetadata.class);

        assertNotNull(agent);
        assertInstanceOf(InternalAgent.class, agent);
        InternalAgent ia = (InternalAgent) agent;
        assertEquals("myAgenticAgent", ia.name());
        assertEquals("an agentic agent", ia.description());
        assertEquals("myOutput", ia.outputKey());
        assertTrue(ia.async());
    }

    // =========================================================================
    // Tool resolution
    // =========================================================================
    @SuppressWarnings("unchecked")
    @Test
    void create_prefersToolProviderOverToolNames() {
        Instance<Object> lookup = prepareLookups();
        Instance<ToolProvider> tp = mock(Instance.class);
        ToolProvider provider = mock(ToolProvider.class);
        when(lookup.select(ToolProvider.class, NamedLiteral.of("tp1"))).thenReturn(tp);
        when(tp.isResolvable()).thenReturn(true);
        when(tp.get()).thenReturn(provider);

        Object agent = CommonAgentCreator.create(lookup, AgentWithToolProvider.class);
        assertNotNull(agent);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_resolvesNamedToolFromCdi() {
        Instance<Object> lookup = prepareLookups();
        Instance<Object> toolInstance = mock(Instance.class);
        when(lookup.select(Object.class, NamedLiteral.of("myTool"))).thenReturn(toolInstance);
        when(toolInstance.isResolvable()).thenReturn(true);
        when(toolInstance.get()).thenReturn(new ToolAImpl());

        Object agent = CommonAgentCreator.create(lookup, AgentWithToolNames.class);
        assertNotNull(agent);
        verify(lookup).select(Object.class, NamedLiteral.of("myTool"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_skipsUnresolvableNamedTool() {
        Instance<Object> lookup = prepareLookups();
        Instance<Object> toolInstance = mock(Instance.class);
        when(lookup.select(Object.class, NamedLiteral.of("myTool"))).thenReturn(toolInstance);
        when(toolInstance.isResolvable()).thenReturn(false);

        Object agent = CommonAgentCreator.create(lookup, AgentWithToolNames.class);
        assertNotNull(agent);
    }

    // =========================================================================
    // Guardrail wiring
    // =========================================================================
    @SuppressWarnings("unchecked")
    @Test
    void create_wiresNamedInputAndOutputGuardrails() {
        Instance<Object> lookup = prepareLookups();

        Instance<InputGuardrail> igInstance = mock(Instance.class);
        when(lookup.select(InputGuardrail.class, NamedLiteral.of("myIG"))).thenReturn(igInstance);
        when(igInstance.isResolvable()).thenReturn(true);
        when(igInstance.get()).thenReturn(new TestInputGuardrail());

        Instance<OutputGuardrail> ogInstance = mock(Instance.class);
        when(lookup.select(OutputGuardrail.class, NamedLiteral.of("myOG"))).thenReturn(ogInstance);
        when(ogInstance.isResolvable()).thenReturn(true);
        when(ogInstance.get()).thenReturn(new TestOutputGuardrail());

        Object agent = CommonAgentCreator.create(lookup, AgentWithNamedGuardrails.class);
        assertNotNull(agent);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_skipsUnresolvableNamedGuardrails() {
        Instance<Object> lookup = prepareLookups();

        Instance<InputGuardrail> igInstance = mock(Instance.class);
        when(lookup.select(InputGuardrail.class, NamedLiteral.of("nonExistent")))
                .thenReturn(igInstance);
        when(igInstance.isResolvable()).thenReturn(false);

        Object agent = CommonAgentCreator.create(lookup, AgentWithUnresolvableNamedGuardrail.class);
        assertNotNull(agent);
    }

    // =========================================================================
    // SEQUENCE topology
    // =========================================================================
    @Test
    void create_sequenceTopology_buildsProxy() {
        Instance<Object> lookup = prepareLookupsWithSubAgents("stepA", "stepB");
        SequenceOrchestrator agent = CommonAgentCreator.create(lookup, SequenceOrchestrator.class);

        assertNotNull(agent);
        verify(lookup).select(Object.class, NamedLiteral.of("stepA"));
        verify(lookup).select(Object.class, NamedLiteral.of("stepB"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_sequenceTopology_skipsUnresolvableSubAgent() {
        Instance<Object> lookup = prepareLookups();
        Instance<Object> missingBean = mock(Instance.class);
        when(lookup.select(Object.class, NamedLiteral.of("stepA"))).thenReturn(missingBean);
        when(lookup.select(Object.class, NamedLiteral.of("stepB"))).thenReturn(missingBean);
        when(missingBean.isResolvable()).thenReturn(false);

        SequenceOrchestrator agent = CommonAgentCreator.create(lookup, SequenceOrchestrator.class);
        assertNotNull(agent);
    }

    // =========================================================================
    // LOOP topology
    // =========================================================================
    @Test
    void create_loopTopology_buildsProxy() {
        Instance<Object> lookup = prepareLookupsWithSubAgents("worker");
        LoopOrchestrator agent = CommonAgentCreator.create(lookup, LoopOrchestrator.class);

        assertNotNull(agent);
        verify(lookup).select(Object.class, NamedLiteral.of("worker"));
    }

    static Stream<Class<?>> predicateExitConditionInterfaces() {
        return Stream.of(
                LoopOrchestratorWithExitCondition.class, LoopOrchestratorWithExitConditionAndDescription.class);
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @MethodSource("predicateExitConditionInterfaces")
    void create_loopTopology_withPredicateExitCondition_wiresPredicateBean(Class<?> type) {
        Instance<Object> lookup = prepareLookupsWithSubAgents("worker");
        Predicate<AgenticScope> predicate = scope -> false;
        Instance<Predicate> predicateInstance = mock(Instance.class);
        when(lookup.select(Predicate.class, NamedLiteral.of("myExitCondition"))).thenReturn(predicateInstance);
        when(predicateInstance.isResolvable()).thenReturn(true);
        when(predicateInstance.get()).thenReturn(predicate);

        @SuppressWarnings("rawtypes")
        Object agent = CommonAgentCreator.create(lookup, (Class) type);

        assertNotNull(agent);
        verify(lookup).select(Predicate.class, NamedLiteral.of("myExitCondition"));
    }

    static Stream<Class<?>> biPredicateExitConditionInterfaces() {
        return Stream.of(
                LoopOrchestratorWithBiPredicateExitCondition.class,
                LoopOrchestratorWithBiPredicateAndDescription.class);
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @MethodSource("biPredicateExitConditionInterfaces")
    void create_loopTopology_withBiPredicateExitCondition_fallsBackFromPredicateAndWiresBiPredicate(Class<?> type) {
        Instance<Object> lookup = prepareLookupsWithSubAgents("worker");
        BiPredicate<AgenticScope, Integer> biPredicate = (scope, i) -> false;
        Instance<Predicate> predicateInstance = mock(Instance.class);
        Instance<BiPredicate> biPredicateInstance = mock(Instance.class);
        when(lookup.select(Predicate.class, NamedLiteral.of("myBiExitCondition")))
                .thenReturn(predicateInstance);
        when(predicateInstance.isResolvable()).thenReturn(false);
        when(lookup.select(BiPredicate.class, NamedLiteral.of("myBiExitCondition")))
                .thenReturn(biPredicateInstance);
        when(biPredicateInstance.isResolvable()).thenReturn(true);
        when(biPredicateInstance.get()).thenReturn(biPredicate);

        @SuppressWarnings("rawtypes")
        Object agent = CommonAgentCreator.create(lookup, (Class) type);

        assertNotNull(agent);
        verify(lookup).select(BiPredicate.class, NamedLiteral.of("myBiExitCondition"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_loopTopology_withTestAfterEachIteration_buildsProxy() {
        Instance<Object> lookup = prepareLookupsWithSubAgents("worker");
        Predicate<AgenticScope> predicate = scope -> false;
        Instance<Predicate> predicateInstance = mock(Instance.class);
        when(lookup.select(Predicate.class, NamedLiteral.of("myExitCondition"))).thenReturn(predicateInstance);
        when(predicateInstance.isResolvable()).thenReturn(true);
        when(predicateInstance.get()).thenReturn(predicate);

        LoopOrchestratorWithTestAfterEachIteration agent =
                CommonAgentCreator.create(lookup, LoopOrchestratorWithTestAfterEachIteration.class);

        assertNotNull(agent);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_loopTopology_withUnresolvableExitCondition_buildsProxyWithoutExitCondition() {
        Instance<Object> lookup = prepareLookupsWithSubAgents("worker");
        Instance<Predicate> predicateInstance = mock(Instance.class);
        Instance<BiPredicate> biPredicateInstance = mock(Instance.class);
        when(lookup.select(Predicate.class, NamedLiteral.of("unresolvableCondition")))
                .thenReturn(predicateInstance);
        when(predicateInstance.isResolvable()).thenReturn(false);
        when(lookup.select(BiPredicate.class, NamedLiteral.of("unresolvableCondition")))
                .thenReturn(biPredicateInstance);
        when(biPredicateInstance.isResolvable()).thenReturn(false);
        Instance<Object> namedObjectInstance = mock(Instance.class);
        when(lookup.select(Object.class, NamedLiteral.of("unresolvableCondition")))
                .thenReturn(namedObjectInstance);
        when(namedObjectInstance.isResolvable()).thenReturn(false);

        LoopOrchestratorWithUnresolvableExitCondition agent =
                CommonAgentCreator.create(lookup, LoopOrchestratorWithUnresolvableExitCondition.class);

        assertNotNull(agent);
    }

    // =========================================================================
    // PARALLEL topology
    // =========================================================================
    @Test
    void create_parallelTopology_buildsProxy() {
        Instance<Object> lookup = prepareLookupsWithSubAgents("taskA", "taskB");
        ParallelOrchestrator agent = CommonAgentCreator.create(lookup, ParallelOrchestrator.class);

        assertNotNull(agent);
        verify(lookup).select(Object.class, NamedLiteral.of("taskA"));
        verify(lookup).select(Object.class, NamedLiteral.of("taskB"));
    }

    // =========================================================================
    // CONDITIONAL topology
    // =========================================================================
    @Test
    void create_conditionalTopology_buildsProxy() {
        Instance<Object> lookup = prepareLookupsWithSubAgents("branchA", "branchB");
        ConditionalOrchestrator agent = CommonAgentCreator.create(lookup, ConditionalOrchestrator.class);

        assertNotNull(agent);
        verify(lookup).select(Object.class, NamedLiteral.of("branchA"));
        verify(lookup).select(Object.class, NamedLiteral.of("branchB"));
    }

    // =========================================================================
    // SUPERVISOR topology
    // =========================================================================
    @Test
    void create_supervisorTopology_buildsProxy() {
        Instance<Object> lookup = prepareLookupsWithSubAgents("workerAgent");
        SupervisorOrchestrator agent = CommonAgentCreator.create(lookup, SupervisorOrchestrator.class);

        assertNotNull(agent);
        verify(lookup).select(Object.class, NamedLiteral.of("workerAgent"));
    }

    @Test
    void create_supervisorTopology_resolvesChatModelFromCdi() {
        Instance<Object> lookup = prepareLookupsWithSubAgents("workerAgent");
        CommonAgentCreator.create(lookup, SupervisorOrchestrator.class);

        // SUPERVISOR wires the ChatModel — verify the CDI lookup for the default bean was made.
        // atLeastOnce() because prepareLookupsWithSubAgents internally creates a SimpleAgent proxy
        // on the same lookup mock, which also resolves the default ChatModel once.
        verify(lookup, atLeastOnce()).select(ChatModel.class);
    }

    // =========================================================================
    // PARALLEL_MAPPER topology
    // =========================================================================
    @Test
    void create_parallelMapperTopology_buildsProxy() {
        Instance<Object> lookup = prepareLookupsWithSubAgents("worker");
        ParallelMapperOrchestrator agent = CommonAgentCreator.create(lookup, ParallelMapperOrchestrator.class);

        assertNotNull(agent);
        verify(lookup).select(Object.class, NamedLiteral.of("worker"));
    }

    @Test
    void create_parallelMapperTopology_throwsWhenItemsKeyMissing() {
        Instance<Object> lookup = prepareLookupsWithSubAgents("worker");
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CommonAgentCreator.create(lookup, ParallelMapperOrchestratorNoItemsKey.class));
        assertTrue(ex.getMessage().contains("itemsKey"));
    }

    // =========================================================================
    // MCP_CLIENT topology
    // =========================================================================

    @Test
    void create_mcpClientTopology_throwsWhenClientNameBlank() {
        Instance<Object> lookup = prepareLookups();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CommonAgentCreator.create(lookup, McpClientAgentBlankClientName.class));
        assertTrue(ex.getMessage().contains("mcpClientName"));
    }

    @Test
    void create_mcpClientTopology_throwsWhenToolNameBlank() {
        Instance<Object> lookup = prepareLookups();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CommonAgentCreator.create(lookup, McpClientAgentBlankToolName.class));
        assertTrue(ex.getMessage().contains("mcpToolName"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_mcpClientTopology_throwsWhenClientBeanNotResolvable() {
        Instance<Object> lookup = prepareLookups();
        Instance<Object> clientInstance = mock(Instance.class);
        when(lookup.select(Object.class, NamedLiteral.of("unknownClient"))).thenReturn(clientInstance);
        when(clientInstance.isResolvable()).thenReturn(false);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> CommonAgentCreator.create(lookup, McpClientAgentUnresolvableClient.class));
        assertTrue(ex.getMessage().contains("unknownClient"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_mcpClientTopology_wiresToolNameAndInputKeys() {
        Instance<Object> lookup = prepareLookups();
        Object mcpClientBean = new Object();
        Instance<Object> clientInstance = mock(Instance.class);
        when(lookup.select(Object.class, NamedLiteral.of("myMcpClient"))).thenReturn(clientInstance);
        when(clientInstance.isResolvable()).thenReturn(true);
        when(clientInstance.get()).thenReturn(mcpClientBean);

        McpClientAgentWithInputKeys mockProxy = mock(McpClientAgentWithInputKeys.class);
        McpClientBuilder<McpClientAgentWithInputKeys> mcpBuilder = mock(McpClientBuilder.class);
        when(mcpBuilder.toolName(anyString())).thenReturn(mcpBuilder);
        when(mcpBuilder.inputKeys(any(String[].class))).thenReturn(mcpBuilder);
        when(mcpBuilder.async(anyBoolean())).thenReturn(mcpBuilder);
        when(mcpBuilder.build()).thenReturn(mockProxy);

        McpService mcpServiceMock = mock(McpService.class);
        when(mcpServiceMock.mcpBuilder(mcpClientBean, McpClientAgentWithInputKeys.class))
                .thenReturn(mcpBuilder);

        try (MockedStatic<McpService> mcpStatic = mockStatic(McpService.class)) {
            mcpStatic.when(McpService::get).thenReturn(mcpServiceMock);

            McpClientAgentWithInputKeys agent = CommonAgentCreator.create(lookup, McpClientAgentWithInputKeys.class);

            assertSame(mockProxy, agent);
            verify(mcpBuilder).toolName("web_search");
            verify(mcpBuilder).inputKeys("query", "locale");
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_mcpClientTopology_wiresAsyncTrue() {
        Instance<Object> lookup = prepareLookups();
        Object mcpClientBean = new Object();
        Instance<Object> clientInstance = mock(Instance.class);
        when(lookup.select(Object.class, NamedLiteral.of("myMcpClient"))).thenReturn(clientInstance);
        when(clientInstance.isResolvable()).thenReturn(true);
        when(clientInstance.get()).thenReturn(mcpClientBean);

        McpClientAgentAsync mockProxy = mock(McpClientAgentAsync.class);
        McpClientBuilder<McpClientAgentAsync> mcpBuilder = mock(McpClientBuilder.class);
        when(mcpBuilder.toolName(anyString())).thenReturn(mcpBuilder);
        when(mcpBuilder.async(anyBoolean())).thenReturn(mcpBuilder);
        when(mcpBuilder.build()).thenReturn(mockProxy);

        McpService mcpServiceMock = mock(McpService.class);
        when(mcpServiceMock.mcpBuilder(mcpClientBean, McpClientAgentAsync.class))
                .thenReturn(mcpBuilder);

        try (MockedStatic<McpService> mcpStatic = mockStatic(McpService.class)) {
            mcpStatic.when(McpService::get).thenReturn(mcpServiceMock);

            McpClientAgentAsync agent = CommonAgentCreator.create(lookup, McpClientAgentAsync.class);

            assertSame(mockProxy, agent);
            verify(mcpBuilder).async(true);
        }
    }

    // =========================================================================
    // HUMAN_IN_THE_LOOP topology
    // =========================================================================
    @Test
    void create_humanInTheLoopTopology_withStaticAskUserMethod_buildsProxy() {
        Instance<Object> lookup = prepareLookups();

        HumanInTheLoopAgent agent = CommonAgentCreator.create(lookup, HumanInTheLoopAgent.class);

        assertNotNull(agent);
        assertInstanceOf(InternalAgent.class, agent);
    }

    @Test
    void create_humanInTheLoopTopology_responseProvider_invokesStaticMethod() {
        Instance<Object> lookup = prepareLookups();
        HumanInTheLoopAgent agent = CommonAgentCreator.create(lookup, HumanInTheLoopAgent.class);
        HumanInTheLoop spec = ((HumanInTheLoopHolder) agent).getHumanInTheLoop();
        AgenticScope scope = mock(AgenticScope.class);

        Object result = spec.responseProvider().apply(scope);

        assertEquals("approved", result);
    }

    @Test
    void create_humanInTheLoopTopology_namedMethod_responseProvider_invokesCorrectMethod() {
        Instance<Object> lookup = prepareLookups();
        HumanInTheLoopNamedMethod agent = CommonAgentCreator.create(lookup, HumanInTheLoopNamedMethod.class);
        HumanInTheLoop spec = ((HumanInTheLoopHolder) agent).getHumanInTheLoop();
        AgenticScope scope = mock(AgenticScope.class);

        Object result = spec.responseProvider().apply(scope);

        assertEquals("named-response", result);
    }

    @Test
    void create_humanInTheLoopTopology_markerInterface_responseProvider_invokesStaticMethod() {
        Instance<Object> lookup = prepareLookups();
        HumanInTheLoopMarkerAgent agent = CommonAgentCreator.create(lookup, HumanInTheLoopMarkerAgent.class);
        HumanInTheLoop spec = ((HumanInTheLoopHolder) agent).getHumanInTheLoop();
        AgenticScope scope = mock(AgenticScope.class);

        Object result = spec.responseProvider().apply(scope);

        assertEquals("approved-by-marker", result);
    }

    @Test
    void create_humanInTheLoopTopology_withNoProvider_throws() {
        Instance<Object> lookup = prepareLookups();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CommonAgentCreator.create(lookup, HumanInTheLoopAgentNoProvider.class));
        assertTrue(ex.getMessage().contains("must declare a static String(AgenticScope) method"));
    }

    @Test
    void create_humanInTheLoopTopology_markerInterface_buildsProxy() {
        Instance<Object> lookup = prepareLookups();
        HumanInTheLoopMarkerAgent agent = CommonAgentCreator.create(lookup, HumanInTheLoopMarkerAgent.class);

        assertNotNull(agent);
        assertInstanceOf(InternalAgent.class, agent);
        InternalAgent ia = (InternalAgent) agent;
        assertEquals("human-approval-agent", ia.name());
        assertEquals("Asks human for approval", ia.description());
        assertEquals("approvalDecision", ia.outputKey());
    }

    @Test
    void toAgentExecutor_humanInTheLoopMarkerInterface_returnsAgentExecutor() {
        Instance<Object> lookup = prepareLookups();
        HumanInTheLoopMarkerAgent proxy = CommonAgentCreator.create(lookup, HumanInTheLoopMarkerAgent.class);
        assertInstanceOf(InternalAgent.class, proxy);

        Object result = CommonAgentCreator.toAgentExecutor(proxy);

        assertInstanceOf(AgentExecutor.class, result);
        assertEquals("human-approval-agent", ((InternalAgent) result).name());
    }

    @Test
    void create_humanInTheLoopTopology_namedAskUser_buildsProxy() {
        Instance<Object> lookup = prepareLookups();

        HumanInTheLoopNamedMethod agent = CommonAgentCreator.create(lookup, HumanInTheLoopNamedMethod.class);

        assertNotNull(agent);
        assertInstanceOf(InternalAgent.class, agent);
    }

    @Test
    void create_humanInTheLoopTopology_badName_throws() {
        Instance<Object> lookup = prepareLookups();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> CommonAgentCreator.create(lookup, HumanInTheLoopBadName.class));
        assertTrue(ex.getMessage().contains("nonExistent"));
    }

    @Test
    void create_humanInTheLoopTopology_ambiguous_throws() {
        Instance<Object> lookup = prepareLookups();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> CommonAgentCreator.create(lookup, HumanInTheLoopAmbiguous.class));
        assertTrue(ex.getMessage().contains("askUser"));
    }

    // =========================================================================
    // PLANNER topology
    // =========================================================================
    @Test
    void create_plannerTopology_buildsProxy() {
        Instance<Object> lookup = prepareLookupsWithSubAgents("plannerWorker");
        // TestPlannerOrchestrator is a public top-level interface — required because
        // DeclarativeUtil.invokeStatic(method) calls method.invoke(null) and the declaring
        // class must be accessible from the langchain4j-agentic named module.
        TestPlannerOrchestrator agent = CommonAgentCreator.create(lookup, TestPlannerOrchestrator.class);

        assertNotNull(agent);
        verify(lookup).select(Object.class, NamedLiteral.of("plannerWorker"));
    }

    @Test
    void create_plannerTopology_throwsWhenPlannerSupplierMissing() {
        Instance<Object> lookup = prepareLookups();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CommonAgentCreator.create(lookup, PlannerOrchestratorWithoutSupplier.class));
        assertTrue(ex.getMessage().contains("plannerName"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_plannerTopology_withPlannerName_wiresPlanner() {
        Instance<Object> lookup = prepareLookupsWithSubAgents("plannerWorker");
        Planner plannerMock = mock(Planner.class);
        Instance<Planner> plannerInstance = mock(Instance.class);
        when(lookup.select(Planner.class, NamedLiteral.of("myPlanner"))).thenReturn(plannerInstance);
        when(plannerInstance.isResolvable()).thenReturn(true);
        when(plannerInstance.get()).thenReturn(plannerMock);

        PlannerOrchestratorWithPlannerName agent =
                CommonAgentCreator.create(lookup, PlannerOrchestratorWithPlannerName.class);

        assertNotNull(agent);
        verify(lookup).select(Planner.class, NamedLiteral.of("myPlanner"));
    }

    @Test
    void create_supervisorTopology_withSupervisorContext_buildsProxy() {
        Instance<Object> lookup = prepareLookupsWithSubAgents("workerAgent");
        SupervisorOrchestratorWithContext agent =
                CommonAgentCreator.create(lookup, SupervisorOrchestratorWithContext.class);

        assertNotNull(agent);
    }

    // =========================================================================
    // A2A topology — requires langchain4j-cdi-a2a on classpath (not present in core tests)
    // =========================================================================
    @Test
    void create_a2aTopology_throwsWhenSpiMissing() {
        Instance<Object> lookup = prepareLookups();
        IllegalStateException ex = assertThrows(
                IllegalStateException.class, () -> CommonAgentCreator.create(lookup, A2AAgentWithUrl.class));
        assertTrue(ex.getMessage().contains("langchain4j-cdi-a2a"));
    }

    @Test
    void create_a2aTopology_throwsWhenUrlBlank() {
        Instance<Object> lookup = prepareLookups();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> CommonAgentCreator.create(lookup, A2AAgentBlankUrl.class));
        assertTrue(ex.getMessage().contains("a2aServerUrl"));
    }

    // =========================================================================
    // Missing annotation
    // =========================================================================
    @Test
    void create_throwsWhenAnnotationMissing() {
        Instance<Object> lookup = prepareLookups();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> CommonAgentCreator.create(lookup, PlainInterface.class));
        assertTrue(ex.getMessage().contains("per-topology stereotype"));
    }

    // =========================================================================
    // toAgentExecutor — Weld CDI proxy simulation
    //
    // Weld (WildFly) generates client proxies as bytecode subclasses:
    //   WeldProxy extends WeldGeneratedBase (which implements UserInterface + InternalAgent)
    // so proxy.getClass().getInterfaces() returns [] — the annotated interface is only found
    // by walking the superclass chain. The two inner classes below reproduce this structure.
    // =========================================================================

    /** Simulates the Weld-generated base class that directly implements the user's agent interface. */
    abstract static class WeldLikeBaseClass implements SimpleAgent, InternalAgent {}

    /**
     * Simulates the actual Weld client proxy: it extends the base class but does NOT declare any interfaces of its own,
     * so {@code getClass().getInterfaces()} returns an empty array. Abstract because Mockito will provide the method
     * implementations.
     */
    abstract static class WeldLikeProxyClass extends WeldLikeBaseClass {}

    @Test
    void toAgentExecutor_withWeldProxyInheritingInterfaceViaSuperclass_returnsAgentExecutor() {
        // Verify that the simulated proxy class does NOT directly implement SimpleAgent
        // (i.e. it's not in getInterfaces()), which is the bug trigger on WildFly.
        assertFalse(
                java.util.Arrays.asList(WeldLikeProxyClass.class.getInterfaces())
                        .contains(SimpleAgent.class),
                "WeldLikeProxyClass must not directly implement SimpleAgent — test precondition");

        // But it IS an InternalAgent (via the superclass), so the instanceof branch is entered.
        WeldLikeProxyClass proxy = mock(WeldLikeProxyClass.class);
        assertInstanceOf(InternalAgent.class, proxy);

        // Before the fix (using getInterfaces() directly), this threw "Agent executor not found".
        // After the fix (allInterfaces walks the superclass chain), it must succeed.
        Object result = CommonAgentCreator.toAgentExecutor(proxy);

        assertInstanceOf(AgentExecutor.class, result);
    }

    // =========================================================================
    // toAgentExecutor
    // =========================================================================
    @Test
    void toAgentExecutor_alreadyAgentExecutor_returnsSame() {
        AgentExecutor executor = mock(AgentExecutor.class);
        Object result = CommonAgentCreator.toAgentExecutor(executor);
        assertSame(executor, result);
    }

    @Test
    void toAgentExecutor_withCdiProxy_returnsAgentExecutor() {
        Instance<Object> lookup = prepareLookups();
        SimpleAgent proxy = CommonAgentCreator.create(lookup, SimpleAgent.class);
        assertInstanceOf(InternalAgent.class, proxy);

        Object result = CommonAgentCreator.toAgentExecutor(proxy);

        assertInstanceOf(AgentExecutor.class, result);
        assertNotSame(proxy, result);
    }

    @Test
    void toAgentExecutor_withNamedCdiProxy_usesAnnotationName() {
        Instance<Object> lookup = prepareLookups();
        NamedAgent proxy = CommonAgentCreator.create(lookup, NamedAgent.class);

        Object result = CommonAgentCreator.toAgentExecutor(proxy);

        assertInstanceOf(AgentExecutor.class, result);
        // NamedAgent declares name = "myAgent" — the resulting AgentExecutor must use it
        assertEquals("myAgent", ((InternalAgent) result).name());
    }

    @Test
    void toAgentExecutor_withExpressionResolvedName_usesResolvedName() {
        Instance<Object> lookup = prepareLookups();
        // ExpressionNamedAgent has name = "${myAgent}"; TestExpressionResolver strips ${} → "myAgent"
        ExpressionNamedAgent proxy = CommonAgentCreator.create(lookup, ExpressionNamedAgent.class);

        Object result = CommonAgentCreator.toAgentExecutor(proxy);

        assertInstanceOf(AgentExecutor.class, result);
        assertEquals("myAgent", ((InternalAgent) result).name());
    }

    @Test
    void toAgentExecutor_withUnnamedCdiProxy_fallsBackToMethodName() {
        Instance<Object> lookup = prepareLookups();
        // SimpleAgent has no name set — should fall back to the entry method name "chat"
        SimpleAgent proxy = CommonAgentCreator.create(lookup, SimpleAgent.class);

        Object result = CommonAgentCreator.toAgentExecutor(proxy);

        assertInstanceOf(AgentExecutor.class, result);
        assertEquals("chat", ((InternalAgent) result).name());
    }

    // =========================================================================
    // findEntryMethod (via reflection)
    // =========================================================================
    @Test
    void findEntryMethod_singleAbstractMethod_returnsIt() throws Exception {
        Method m = getFindEntryMethod();
        Method result = (Method) m.invoke(null, SingleMethodInterface.class);
        assertNotNull(result);
        assertEquals("doSomething", result.getName());
    }

    @Test
    void findEntryMethod_multipleAbstractMethods_throwsIllegalArgument() throws Exception {
        Method m = getFindEntryMethod();
        InvocationTargetException ex =
                assertThrows(InvocationTargetException.class, () -> m.invoke(null, MultiMethodInterface.class));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("must declare exactly one abstract method"));
    }

    @Test
    void findEntryMethod_onlyDefaultMethods_returnsNull() throws Exception {
        Method m = getFindEntryMethod();
        Method result = (Method) m.invoke(null, OnlyDefaultMethodsInterface.class);
        assertNull(result);
    }

    @Test
    void findEntryMethod_mixedMethods_skipsDefaultReturnsAbstract() throws Exception {
        Method m = getFindEntryMethod();
        Method result = (Method) m.invoke(null, MixedMethodsInterface.class);
        assertNotNull(result);
        assertEquals("process", result.getName());
    }

    // =========================================================================
    // CdiLookupHelper.getInstance
    // =========================================================================
    @SuppressWarnings("unchecked")
    @Test
    void getInstance_returnsNullWhenNameBlank() {
        Instance<Object> lookup = mock(Instance.class);
        assertNull(CdiLookupHelper.getInstance(lookup, ChatModel.class, ""));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getInstance_returnsNullWhenNameNull() {
        Instance<Object> lookup = mock(Instance.class);
        assertNull(CdiLookupHelper.getInstance(lookup, ChatModel.class, null));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getInstance_returnsDefaultBeanWhenNameIsHashDefault() {
        Instance<Object> lookup = mock(Instance.class);
        Instance<ChatModel> cmInstance = mock(Instance.class);
        when(lookup.select(ChatModel.class)).thenReturn(cmInstance);

        assertSame(cmInstance, CdiLookupHelper.getInstance(lookup, ChatModel.class, "#default"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getInstance_returnsNamedBeanWhenNameSpecific() {
        Instance<Object> lookup = mock(Instance.class);
        Instance<ChatModel> cmInstance = mock(Instance.class);
        when(lookup.select(ChatModel.class, NamedLiteral.of("myModel"))).thenReturn(cmInstance);

        assertSame(cmInstance, CdiLookupHelper.getInstance(lookup, ChatModel.class, "myModel"));
    }

    // =========================================================================
    // ExpressionResolver SPI — CdiLookupHelper.resolveExpression
    // =========================================================================
    @Test
    void resolveExpression_passesThroughPlainString() {
        // Plain names must not be altered by the test resolver (no ${} delimiters)
        assertEquals("plainValue", CdiLookupHelper.resolveExpression("plainValue"));
        assertEquals("#default", CdiLookupHelper.resolveExpression("#default"));
        assertEquals("mem1", CdiLookupHelper.resolveExpression("mem1"));
    }

    @Test
    void resolveExpression_returnsNullWhenNull() {
        assertNull(CdiLookupHelper.resolveExpression(null));
    }

    @Test
    void resolveExpression_returnsBlankWhenBlank() {
        assertEquals("", CdiLookupHelper.resolveExpression(""));
    }

    @Test
    void resolveExpression_stripsDelimitersViaTestResolver() {
        // TestExpressionResolver (registered in test META-INF/services) strips ${...}
        assertEquals("#default", CdiLookupHelper.resolveExpression("${#default}"));
        assertEquals("mem1", CdiLookupHelper.resolveExpression("${mem1}"));
        assertEquals("myAgent", CdiLookupHelper.resolveExpression("${myAgent}"));
    }

    @Test
    void create_resolvesExpressionInChatModelName() {
        // ${#default} resolves to #default via TestExpressionResolver
        Instance<Object> lookup = prepareLookups();
        ExpressionChatModelAgent agent = CommonAgentCreator.create(lookup, ExpressionChatModelAgent.class);
        assertNotNull(agent);
        assertInstanceOf(InternalAgent.class, agent);
    }

    @Test
    void create_resolvesExpressionInName() {
        // ${myAgent} resolves to myAgent via TestExpressionResolver
        Instance<Object> lookup = prepareLookups();
        ExpressionNamedAgent agent = CommonAgentCreator.create(lookup, ExpressionNamedAgent.class);
        assertNotNull(agent);
        assertTrue(agent.toString().contains("Agent[myAgent]"));
    }

    // =========================================================================
    // Reflection helpers
    // =========================================================================
    private static Method getFindEntryMethod() throws Exception {
        Method m = CommonAgentCreator.class.getDeclaredMethod("findEntryMethod", Class.class);
        m.setAccessible(true);
        return m;
    }
}
