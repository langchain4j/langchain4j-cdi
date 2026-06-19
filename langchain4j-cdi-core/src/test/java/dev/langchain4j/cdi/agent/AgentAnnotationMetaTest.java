package dev.langchain4j.cdi.agent;

import static org.junit.jupiter.api.Assertions.*;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.TypedKey;
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
import jakarta.enterprise.context.ApplicationScoped;
import org.junit.jupiter.api.Test;

@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
class AgentAnnotationMetaTest {

    // --- One annotated interface per topology ---

    @RegisterSimpleAgent(name = "simple", description = "simple desc", outputKey = "simpleOut", async = true)
    interface SimpleInterface {}

    @RegisterSequenceAgent(name = "seq", description = "seq desc", outputKey = "seqOut")
    interface SequenceInterface {}

    @RegisterLoopAgent(name = "loop", description = "loop desc", outputKey = "loopOut")
    interface LoopInterface {}

    @RegisterParallelAgent(name = "par", description = "par desc", outputKey = "parOut")
    interface ParallelInterface {}

    @RegisterParallelMapperAgent(
            name = "mapper",
            description = "mapper desc",
            outputKey = "mapOut",
            subAgentNames = {"worker"},
            itemsKey = "items")
    interface ParallelMapperInterface {}

    @RegisterConditionalAgent(name = "cond", description = "cond desc", outputKey = "condOut")
    interface ConditionalInterface {}

    @RegisterSupervisorAgent(name = "sup", description = "sup desc", outputKey = "supOut")
    interface SupervisorInterface {}

    @RegisterPlannerAgent(name = "plan", description = "plan desc", outputKey = "planOut")
    interface PlannerInterface {}

    @RegisterA2AAgent(
            name = "a2a",
            description = "a2a desc",
            outputKey = "a2aOut",
            a2aServerUrl = "http://remote-agent:8080",
            async = true)
    interface A2AInterface {}

    @RegisterMcpClientAgent(
            name = "mcp",
            description = "mcp desc",
            outputKey = "mcpOut",
            mcpClientName = "myMcpClient",
            mcpToolName = "web_search")
    interface McpClientInterface {}

    @RegisterHumanInTheLoopAgent(name = "hitl", description = "hitl desc", outputKey = "hitlOut", async = true)
    interface HumanInTheLoopInterface {}

    @RegisterSimpleAgent(
            name = "optSimple",
            description = "optional simple",
            optional = true,
            summarizedContext = {"agentA", "agentB"})
    interface OptionalSimpleInterface {}

    @RegisterSequenceAgent(
            name = "optSeq",
            optional = true,
            summarizedContext = {"writer"})
    interface OptionalSequenceInterface {}

    static class ResultKey implements TypedKey<String> {
        @Override
        public String name() {
            return "typedResult";
        }
    }

    @RegisterSimpleAgent(name = "typedSimple", description = "typed simple", typedOutputKey = ResultKey.class)
    interface TypedSimpleInterface {}

    @RegisterSequenceAgent(name = "typedSeq", typedOutputKey = ResultKey.class)
    interface TypedSequenceInterface {}

    interface PlainInterface {}

    // =========================================================================
    // detect() — null for unannotated or non-interface
    // =========================================================================

    @Test
    void detect_unannotatedInterface_returnsNull() {
        assertNull(AgentAnnotationMeta.detect(PlainInterface.class));
    }

    @Test
    void detect_concreteClass_returnsNull() {
        assertNull(AgentAnnotationMeta.detect(Object.class));
    }

    // =========================================================================
    // detect() — each topology sets the correct annotationClass
    // =========================================================================

    @Test
    void detect_simpleAgent_returnsCorrectMeta() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(SimpleInterface.class);

        assertNotNull(meta);
        assertEquals(RegisterSimpleAgent.class, meta.annotationClass());
        assertEquals("simple", meta.rawName());
        assertEquals("simple desc", meta.rawDescription());
        assertEquals("simpleOut", meta.rawOutputKey());
        assertTrue(meta.async());
        assertEquals(ApplicationScoped.class, meta.scope());
    }

    @Test
    void detect_sequenceAgent_setsAnnotationClassAndNoAsync() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(SequenceInterface.class);

        assertNotNull(meta);
        assertEquals(RegisterSequenceAgent.class, meta.annotationClass());
        assertEquals("seq", meta.rawName());
        assertFalse(meta.async());
    }

    @Test
    void detect_loopAgent_setsAnnotationClassAndNoAsync() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(LoopInterface.class);

        assertNotNull(meta);
        assertEquals(RegisterLoopAgent.class, meta.annotationClass());
        assertFalse(meta.async());
    }

    @Test
    void detect_parallelAgent_setsAnnotationClassAndNoAsync() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(ParallelInterface.class);

        assertNotNull(meta);
        assertEquals(RegisterParallelAgent.class, meta.annotationClass());
        assertFalse(meta.async());
    }

    @Test
    void detect_parallelMapperAgent_setsAnnotationClassAndNoAsync() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(ParallelMapperInterface.class);

        assertNotNull(meta);
        assertEquals(RegisterParallelMapperAgent.class, meta.annotationClass());
        assertEquals("mapper", meta.rawName());
        assertEquals("mapOut", meta.rawOutputKey());
        assertFalse(meta.async());
    }

    @Test
    void detect_conditionalAgent_setsAnnotationClassAndNoAsync() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(ConditionalInterface.class);

        assertNotNull(meta);
        assertEquals(RegisterConditionalAgent.class, meta.annotationClass());
        assertFalse(meta.async());
    }

    @Test
    void detect_supervisorAgent_setsAnnotationClassAndNoAsync() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(SupervisorInterface.class);

        assertNotNull(meta);
        assertEquals(RegisterSupervisorAgent.class, meta.annotationClass());
        assertFalse(meta.async());
    }

    @Test
    void detect_plannerAgent_setsAnnotationClassAndNoAsync() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(PlannerInterface.class);

        assertNotNull(meta);
        assertEquals(RegisterPlannerAgent.class, meta.annotationClass());
        assertFalse(meta.async());
    }

    @Test
    void detect_a2aAgent_setsAnnotationClassAndReflectsAsync() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(A2AInterface.class);

        assertNotNull(meta);
        assertEquals(RegisterA2AAgent.class, meta.annotationClass());
        assertEquals("a2a", meta.rawName());
        assertEquals("a2aOut", meta.rawOutputKey());
        assertTrue(meta.async());
    }

    @Test
    void detect_mcpClientAgent_setsAnnotationClassAndOutputKey() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(McpClientInterface.class);

        assertNotNull(meta);
        assertEquals(RegisterMcpClientAgent.class, meta.annotationClass());
        assertEquals("mcp", meta.rawName());
        // outputKey is supported on MCP_CLIENT despite old README claim to the contrary
        assertEquals("mcpOut", meta.rawOutputKey());
    }

    @Test
    void detect_humanInTheLoopAgent_setsAnnotationClassAndReflectsAsync() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(HumanInTheLoopInterface.class);

        assertNotNull(meta);
        assertEquals(RegisterHumanInTheLoopAgent.class, meta.annotationClass());
        assertEquals("hitl", meta.rawName());
        assertTrue(meta.async());
    }

    // =========================================================================
    // name(), description() and outputKey() — resolved via CdiLookupHelper
    // (no EL/Config active in unit tests, so raw values pass through unchanged)
    // =========================================================================

    @Test
    void name_withoutExpressionResolver_returnsRawValue() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(SequenceInterface.class);

        assertNotNull(meta);
        assertEquals(meta.rawName(), meta.name());
    }

    @Test
    void description_withoutExpressionResolver_returnsRawValue() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(SequenceInterface.class);

        assertNotNull(meta);
        assertEquals(meta.rawDescription(), meta.description());
    }

    @Test
    void outputKey_withoutExpressionResolver_returnsRawValue() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(SequenceInterface.class);

        assertNotNull(meta);
        assertEquals(meta.rawOutputKey(), meta.outputKey());
    }

    // =========================================================================
    // typedOutputKey
    // =========================================================================

    @Test
    void detect_typedOutputKey_extractsClass() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(TypedSimpleInterface.class);

        assertNotNull(meta);
        assertEquals(ResultKey.class, meta.rawTypedOutputKey());
        assertTrue(meta.hasTypedOutputKey());
    }

    @Test
    void detect_typedOutputKey_composedAgent() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(TypedSequenceInterface.class);

        assertNotNull(meta);
        assertEquals(ResultKey.class, meta.rawTypedOutputKey());
        assertTrue(meta.hasTypedOutputKey());
    }

    @Test
    void detect_defaultTypedOutputKey_isNoTypedKey() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(SimpleInterface.class);

        assertNotNull(meta);
        assertEquals(Agent.NoTypedKey.class, meta.rawTypedOutputKey());
        assertFalse(meta.hasTypedOutputKey());
    }

    @Test
    void detect_allDefaultAnnotations_haveNoTypedOutputKey() {
        assertAll(
                () -> assertFalse(
                        AgentAnnotationMeta.detect(SimpleInterface.class).hasTypedOutputKey()),
                () -> assertFalse(
                        AgentAnnotationMeta.detect(SequenceInterface.class).hasTypedOutputKey()),
                () -> assertFalse(
                        AgentAnnotationMeta.detect(LoopInterface.class).hasTypedOutputKey()),
                () -> assertFalse(
                        AgentAnnotationMeta.detect(ParallelInterface.class).hasTypedOutputKey()),
                () -> assertFalse(AgentAnnotationMeta.detect(ParallelMapperInterface.class)
                        .hasTypedOutputKey()),
                () -> assertFalse(
                        AgentAnnotationMeta.detect(ConditionalInterface.class).hasTypedOutputKey()),
                () -> assertFalse(
                        AgentAnnotationMeta.detect(SupervisorInterface.class).hasTypedOutputKey()),
                () -> assertFalse(
                        AgentAnnotationMeta.detect(PlannerInterface.class).hasTypedOutputKey()),
                () -> assertFalse(AgentAnnotationMeta.detect(A2AInterface.class).hasTypedOutputKey()),
                () -> assertFalse(
                        AgentAnnotationMeta.detect(McpClientInterface.class).hasTypedOutputKey()),
                () -> assertFalse(AgentAnnotationMeta.detect(HumanInTheLoopInterface.class)
                        .hasTypedOutputKey()));
    }

    // =========================================================================
    // isAgentInterface()
    // =========================================================================

    @Test
    void isAgentInterface_annotatedInterface_returnsTrue() {
        assertAll(
                () -> assertTrue(AgentAnnotationMeta.isAgentInterface(SimpleInterface.class)),
                () -> assertTrue(AgentAnnotationMeta.isAgentInterface(SequenceInterface.class)),
                () -> assertTrue(AgentAnnotationMeta.isAgentInterface(LoopInterface.class)),
                () -> assertTrue(AgentAnnotationMeta.isAgentInterface(ParallelInterface.class)),
                () -> assertTrue(AgentAnnotationMeta.isAgentInterface(ParallelMapperInterface.class)),
                () -> assertTrue(AgentAnnotationMeta.isAgentInterface(ConditionalInterface.class)),
                () -> assertTrue(AgentAnnotationMeta.isAgentInterface(SupervisorInterface.class)),
                () -> assertTrue(AgentAnnotationMeta.isAgentInterface(PlannerInterface.class)),
                () -> assertTrue(AgentAnnotationMeta.isAgentInterface(A2AInterface.class)),
                () -> assertTrue(AgentAnnotationMeta.isAgentInterface(McpClientInterface.class)),
                () -> assertTrue(AgentAnnotationMeta.isAgentInterface(HumanInTheLoopInterface.class)));
    }

    @Test
    void isAgentInterface_unannotatedInterface_returnsFalse() {
        assertFalse(AgentAnnotationMeta.isAgentInterface(PlainInterface.class));
    }

    @Test
    void isAgentInterface_annotatedClass_returnsFalse() {
        assertFalse(AgentAnnotationMeta.isAgentInterface(Object.class));
    }

    // =========================================================================
    // optional() and summarizedContext()
    // =========================================================================

    @Test
    void detect_simpleAgent_defaultOptionalIsFalse() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(SimpleInterface.class);

        assertNotNull(meta);
        assertFalse(meta.optional());
        assertArrayEquals(new String[0], meta.summarizedContext());
    }

    @Test
    void detect_optionalSimple_setsOptionalAndSummarizedContext() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(OptionalSimpleInterface.class);

        assertNotNull(meta);
        assertTrue(meta.optional());
        assertArrayEquals(new String[] {"agentA", "agentB"}, meta.rawSummarizedContext());
        assertArrayEquals(new String[] {"agentA", "agentB"}, meta.summarizedContext());
    }

    @Test
    void detect_optionalSequence_setsOptionalAndSummarizedContext() {
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(OptionalSequenceInterface.class);

        assertNotNull(meta);
        assertTrue(meta.optional());
        assertArrayEquals(new String[] {"writer"}, meta.summarizedContext());
    }

    @Test
    void detect_composedAgentDefault_optionalIsFalse() {
        assertAll(
                () -> assertFalse(
                        AgentAnnotationMeta.detect(SequenceInterface.class).optional()),
                () -> assertFalse(
                        AgentAnnotationMeta.detect(LoopInterface.class).optional()),
                () -> assertFalse(
                        AgentAnnotationMeta.detect(ParallelInterface.class).optional()),
                () -> assertFalse(AgentAnnotationMeta.detect(ParallelMapperInterface.class)
                        .optional()),
                () -> assertFalse(
                        AgentAnnotationMeta.detect(ConditionalInterface.class).optional()),
                () -> assertFalse(
                        AgentAnnotationMeta.detect(SupervisorInterface.class).optional()),
                () -> assertFalse(
                        AgentAnnotationMeta.detect(PlannerInterface.class).optional()),
                () -> assertFalse(AgentAnnotationMeta.detect(A2AInterface.class).optional()),
                () -> assertFalse(
                        AgentAnnotationMeta.detect(McpClientInterface.class).optional()),
                () -> assertFalse(AgentAnnotationMeta.detect(HumanInTheLoopInterface.class)
                        .optional()));
    }

    @Test
    void detect_composedAgentDefault_summarizedContextIsEmpty() {
        assertAll(
                () -> assertEquals(
                        0, AgentAnnotationMeta.detect(SequenceInterface.class).summarizedContext().length),
                () -> assertEquals(
                        0, AgentAnnotationMeta.detect(LoopInterface.class).summarizedContext().length),
                () -> assertEquals(
                        0, AgentAnnotationMeta.detect(A2AInterface.class).summarizedContext().length));
    }
}
