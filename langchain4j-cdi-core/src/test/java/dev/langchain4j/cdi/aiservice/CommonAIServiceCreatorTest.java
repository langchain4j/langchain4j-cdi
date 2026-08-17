package dev.langchain4j.cdi.aiservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.cdi.spi.RegisterAIService;
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
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.observability.api.listener.AiServiceResponseReceivedListener;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommonAIServiceCreatorTest {

    @BeforeEach
    void resetStaticState() {
        CAPTURED_THINKING.set(null);
        THROWING_HANDLER_CALLED.set(false);
    }

    interface ToolA {
        String ping();
    }

    static class ToolAImpl implements ToolA {
        public ToolAImpl() {}

        @Tool
        public String ping() {
            return "pong";
        }
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(
            tools = {ToolAImpl.class},
            chatModelName = "#default",
            contentRetrieverName = "cr1",
            retrievalAugmentorName = "ra1",
            toolProviderName = "",
            chatMemoryName = "mem1",
            chatMemoryProviderName = "cmp1",
            moderationModelName = "mod1")
    interface MyAIService {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_buildsAiService_wiringAllResolvableDependencies_andFallsBackToToolsWhenNoToolProvider() {
        Instance<Object> lookup = prepareLookups();

        MyAIService service = CommonAIServiceCreator.create(lookup, MyAIService.class);
        assertNotNull(service);
        // We can't directly introspect AiServices internals; instead we call toString and ensure proxy was made.
        assertTrue(service.toString().contains("MyAIService"));

        // Ensure new tools instance can be created via no-arg ctor path
        // Nothing to assert directly, but at least ensure no exceptions and that both code paths are exercised.
    }

    private static Instance<Object> prepareLookups() {
        Instance<Object> lookup = mock(Instance.class);
        // Prepare instances
        Instance<ChatModel> cm = mock(Instance.class);
        Instance<StreamingChatModel> scm = mock(Instance.class);
        Instance<ContentRetriever> cr = mock(Instance.class);
        Instance<RetrievalAugmentor> ra = mock(Instance.class);
        Instance<ToolProvider> tp = mock(Instance.class);
        Instance<ChatMemory> mem = mock(Instance.class);
        Instance<ChatMemoryProvider> cmp = mock(Instance.class);
        Instance<ModerationModel> mod = mock(Instance.class);

        ChatModel cmBean = mock(ChatModel.class);
        StreamingChatModel scmBean = mock(StreamingChatModel.class);
        ContentRetriever crBean = mock(ContentRetriever.class);
        RetrievalAugmentor raBean = mock(RetrievalAugmentor.class);
        ChatMemory memBean = mock(ChatMemory.class);
        ChatMemoryProvider cmpBean = mock(ChatMemoryProvider.class);
        ModerationModel modBean = mock(ModerationModel.class);

        // lookup.select for names
        when(lookup.select(ChatModel.class)).thenReturn(cm);
        when(lookup.select(StreamingChatModel.class, NamedLiteral.of("stream1")))
                .thenReturn(scm);
        when(lookup.select(ContentRetriever.class, NamedLiteral.of("cr1"))).thenReturn(cr);
        when(lookup.select(RetrievalAugmentor.class, NamedLiteral.of("ra1"))).thenReturn(ra);
        when(lookup.select(ToolProvider.class, NamedLiteral.of("")))
                .thenReturn(tp); // not used since blank name returns null in code
        when(lookup.select(ChatMemory.class, NamedLiteral.of("mem1"))).thenReturn(mem);
        when(lookup.select(ChatMemoryProvider.class, NamedLiteral.of("cmp1"))).thenReturn(cmp);
        when(lookup.select(ModerationModel.class, NamedLiteral.of("mod1"))).thenReturn(mod);

        when(cm.isResolvable()).thenReturn(true);
        when(scm.isResolvable()).thenReturn(true);
        when(cr.isResolvable()).thenReturn(true);
        when(ra.isResolvable()).thenReturn(true);
        when(tp.isResolvable()).thenReturn(false); // so that tools[] path is used
        when(mem.isResolvable()).thenReturn(true);
        when(cmp.isResolvable()).thenReturn(true);
        when(mod.isResolvable()).thenReturn(true);

        when(cm.get()).thenReturn(cmBean);
        when(scm.get()).thenReturn(scmBean);
        when(cr.get()).thenReturn(crBean);
        when(ra.get()).thenReturn(raBean);
        when(mem.get()).thenReturn(memBean);
        when(cmp.get()).thenReturn(cmpBean);
        when(mod.get()).thenReturn(modBean);
        return lookup;
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(
            toolProviderName = "provider1",
            tools = {})
    interface MyAIServiceWithToolProvider {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_prefersToolProviderOverToolsArray() {
        Instance<Object> lookup = prepareLookups();
        Instance<ToolProvider> tp = mock(Instance.class);
        ToolProvider provider = mock(ToolProvider.class);
        when(lookup.select(ToolProvider.class, NamedLiteral.of("provider1"))).thenReturn(tp);
        when(tp.isResolvable()).thenReturn(true);
        when(tp.get()).thenReturn(provider);
        // Other lookups return null by default because names are blank -> getInstance returns null
        Object service = CommonAIServiceCreator.create(lookup, MyAIServiceWithToolProvider.class);
        assertNotNull(service);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(
            toolProviderName = "provider1",
            tools = {ToolAImpl.class})
    interface MyAIServiceWithBothToolProviderAndToolsArray {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_wiresToolProviderAndToolsArraySimultaneously() {
        Instance<Object> lookup = prepareLookups();
        Instance<ToolProvider> tp = mock(Instance.class);
        ToolProvider provider = mock(ToolProvider.class);
        when(lookup.select(ToolProvider.class, NamedLiteral.of("provider1"))).thenReturn(tp);
        when(tp.isResolvable()).thenReturn(true);
        when(tp.get()).thenReturn(provider);

        // Both toolProvider and tools[] are set; service creation must succeed
        Object service = CommonAIServiceCreator.create(lookup, MyAIServiceWithBothToolProviderAndToolsArray.class);
        assertNotNull(service);
        assertTrue(service.toString().contains("MyAIServiceWithBothToolProviderAndToolsArray"));
    }

    // --- Guardrail test classes ---

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

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(
            inputGuardrails = {TestInputGuardrail.class},
            outputGuardrails = {TestOutputGuardrail.class})
    interface MyAIServiceWithGuardrails {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_wiresInputAndOutputGuardrailsFromAnnotation() {
        Instance<Object> lookup = prepareLookups();

        // Mock guardrail CDI lookups
        Instance<TestInputGuardrail> igInstance = mock(Instance.class);
        when(lookup.select(TestInputGuardrail.class)).thenReturn(igInstance);
        when(igInstance.isResolvable()).thenReturn(true);
        when(igInstance.get()).thenReturn(new TestInputGuardrail());

        Instance<TestOutputGuardrail> ogInstance = mock(Instance.class);
        when(lookup.select(TestOutputGuardrail.class)).thenReturn(ogInstance);
        when(ogInstance.isResolvable()).thenReturn(true);
        when(ogInstance.get()).thenReturn(new TestOutputGuardrail());

        Object service = CommonAIServiceCreator.create(lookup, MyAIServiceWithGuardrails.class);
        assertNotNull(service);
        assertTrue(service.toString().contains("MyAIServiceWithGuardrails"));
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(inputGuardrails = {TestInputGuardrail.class})
    interface MyAIServiceWithInputGuardrailsOnly {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_wiresOnlyInputGuardrailsWhenNoOutputGuardrails() {
        Instance<Object> lookup = prepareLookups();

        Instance<TestInputGuardrail> igInstance = mock(Instance.class);
        when(lookup.select(TestInputGuardrail.class)).thenReturn(igInstance);
        when(igInstance.isResolvable()).thenReturn(true);
        when(igInstance.get()).thenReturn(new TestInputGuardrail());

        Object service = CommonAIServiceCreator.create(lookup, MyAIServiceWithInputGuardrailsOnly.class);
        assertNotNull(service);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(inputGuardrails = {TestInputGuardrail.class})
    interface MyAIServiceGuardrailFallback {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_fallsBackToConstructorWhenGuardrailBeanNotResolvable() {
        Instance<Object> lookup = prepareLookups();

        Instance<TestInputGuardrail> igInstance = mock(Instance.class);
        when(lookup.select(TestInputGuardrail.class)).thenReturn(igInstance);
        when(igInstance.isResolvable()).thenReturn(false);

        // Should not throw - falls back to no-arg constructor
        Object service = CommonAIServiceCreator.create(lookup, MyAIServiceGuardrailFallback.class);
        assertNotNull(service);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(
            inputGuardrails = {},
            outputGuardrails = {})
    interface ServiceWithEmptyGuardrailArrays {
        String chat(String question);
    }

    @Test
    void create_withEmptyGuardrailArrays_createsServiceSuccessfully() {
        Instance<Object> lookup = prepareLookups();
        Object service = CommonAIServiceCreator.create(lookup, ServiceWithEmptyGuardrailArrays.class);
        assertNotNull(service);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(
            inputGuardrails = {TestInputGuardrail.class},
            inputGuardrailNames = {"someGuardrail"})
    interface ServiceWithBothInputGuardrailConfigs {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_withBothInputGuardrailConfigsSpecified_usesClassesAndIgnoresNames() {
        Instance<Object> lookup = prepareLookups();

        Instance<TestInputGuardrail> igInstance = mock(Instance.class);
        when(lookup.select(TestInputGuardrail.class)).thenReturn(igInstance);
        when(igInstance.isResolvable()).thenReturn(true);
        when(igInstance.get()).thenReturn(new TestInputGuardrail());

        Object service = CommonAIServiceCreator.create(lookup, ServiceWithBothInputGuardrailConfigs.class);
        assertNotNull(service);
        // The warning should be logged (would need log handler to verify)
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(
            outputGuardrails = {TestOutputGuardrail.class},
            outputGuardrailNames = {"someOutputGuardrail"})
    interface ServiceWithBothOutputGuardrailConfigs {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_withBothOutputGuardrailConfigsSpecified_usesClassesAndIgnoresNames() {
        Instance<Object> lookup = prepareLookups();

        Instance<TestOutputGuardrail> ogInstance = mock(Instance.class);
        when(lookup.select(TestOutputGuardrail.class)).thenReturn(ogInstance);
        when(ogInstance.isResolvable()).thenReturn(true);
        when(ogInstance.get()).thenReturn(new TestOutputGuardrail());

        Object service = CommonAIServiceCreator.create(lookup, ServiceWithBothOutputGuardrailConfigs.class);
        assertNotNull(service);
        // The warning should be logged (would need log handler to verify)
    }

    // --- Named guardrail tests ---

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(
            inputGuardrailNames = {"myInputGuardrail"},
            outputGuardrailNames = {"myOutputGuardrail"})
    interface MyAIServiceWithNamedGuardrails {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_wiresNamedInputAndOutputGuardrails() {
        Instance<Object> lookup = prepareLookups();

        Instance<InputGuardrail> igInstance = mock(Instance.class);
        when(lookup.select(InputGuardrail.class, NamedLiteral.of("myInputGuardrail")))
                .thenReturn(igInstance);
        when(igInstance.isResolvable()).thenReturn(true);
        when(igInstance.get()).thenReturn(new TestInputGuardrail());

        Instance<OutputGuardrail> ogInstance = mock(Instance.class);
        when(lookup.select(OutputGuardrail.class, NamedLiteral.of("myOutputGuardrail")))
                .thenReturn(ogInstance);
        when(ogInstance.isResolvable()).thenReturn(true);
        when(ogInstance.get()).thenReturn(new TestOutputGuardrail());

        Object service = CommonAIServiceCreator.create(lookup, MyAIServiceWithNamedGuardrails.class);
        assertNotNull(service);
        assertTrue(service.toString().contains("MyAIServiceWithNamedGuardrails"));
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(inputGuardrailNames = {"nonExistentGuardrail"})
    interface MyAIServiceWithUnresolvableNamedGuardrail {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_skipsUnresolvableNamedGuardrails() {
        Instance<Object> lookup = prepareLookups();

        Instance<InputGuardrail> igInstance = mock(Instance.class);
        when(lookup.select(InputGuardrail.class, NamedLiteral.of("nonExistentGuardrail")))
                .thenReturn(igInstance);
        when(igInstance.isResolvable()).thenReturn(false);

        // Should not throw - unresolvable names are skipped with a warning
        Object service = CommonAIServiceCreator.create(lookup, MyAIServiceWithUnresolvableNamedGuardrail.class);
        assertNotNull(service);
    }

    // --- Uninstantiable guardrail test ---

    public static class UninstantiableGuardrail implements InputGuardrail {
        public UninstantiableGuardrail(String required) {}

        @Override
        public InputGuardrailResult validate(UserMessage userMessage) {
            return success();
        }
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(inputGuardrails = {UninstantiableGuardrail.class})
    interface MyAIServiceWithUninstantiableGuardrail {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_skipsGuardrailWhenBothCdiAndConstructorFail() {
        Instance<Object> lookup = prepareLookups();

        Instance<UninstantiableGuardrail> igInstance = mock(Instance.class);
        when(lookup.select(UninstantiableGuardrail.class)).thenReturn(igInstance);
        when(igInstance.isResolvable()).thenReturn(false);

        Logger logger = Logger.getLogger(CdiLookupHelper.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        logger.addHandler(handler);
        try {
            Object service = CommonAIServiceCreator.create(lookup, MyAIServiceWithUninstantiableGuardrail.class);
            assertNotNull(service);
            assertTrue(
                    records.stream().noneMatch(r -> r.getLevel().intValue() >= Level.WARNING.intValue()),
                    "No WARNING should be logged for a missing no-arg constructor — it is an expected fallback");
        } finally {
            logger.removeHandler(handler);
        }
    }

    // --- Uninstantiable tool test ---

    public static class UninstantiableTool {
        public UninstantiableTool(String required) {}

        @Tool
        public String doWork() {
            return "unreachable";
        }
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(tools = {UninstantiableTool.class})
    interface MyAIServiceWithUninstantiableTool {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_skipsToolWhenBothCdiAndConstructorFail() {
        Instance<Object> lookup = prepareLookups();

        Instance<UninstantiableTool> toolInstance = mock(Instance.class);
        when(lookup.select(UninstantiableTool.class)).thenReturn(toolInstance);
        when(toolInstance.isResolvable()).thenReturn(false);

        Logger logger = Logger.getLogger(CdiLookupHelper.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        logger.addHandler(handler);
        try {
            Object service = CommonAIServiceCreator.create(lookup, MyAIServiceWithUninstantiableTool.class);
            assertNotNull(service);
            assertTrue(
                    records.stream().noneMatch(r -> r.getLevel().intValue() >= Level.WARNING.intValue()),
                    "No WARNING should be logged for a missing no-arg constructor — it is an expected fallback");
        } finally {
            logger.removeHandler(handler);
        }
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(chatModelName = "")
    interface MisconfiguredService {
        String chat(String question);
    }

    @Test
    void getInstance_returnsNullWhenNameBlank() {
        @SuppressWarnings("unchecked")
        Instance<Object> lookup = mock(Instance.class);
        assertNull(CdiLookupHelper.getInstance(lookup, ChatModel.class, ""));
    }

    // --- listenerNames tests ---

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(listenerNames = {"myListener"})
    interface MyAIServiceWithListener {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_wiresNamedListeners() {
        Instance<Object> lookup = prepareLookups();

        Instance<Object> listenerInstance = mock(Instance.class);
        AiServiceResponseReceivedListener listener = event -> {};
        when(lookup.select(Object.class, NamedLiteral.of("myListener"))).thenReturn(listenerInstance);
        when(listenerInstance.isResolvable()).thenReturn(true);
        when(listenerInstance.get()).thenReturn(listener);

        Object service = CommonAIServiceCreator.create(lookup, MyAIServiceWithListener.class);
        assertNotNull(service);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService(listenerNames = {"nonExistentListener"})
    interface MyAIServiceWithUnresolvableListener {
        String chat(String question);
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_skipsUnresolvableNamedListeners() {
        Instance<Object> lookup = prepareLookups();

        Instance<Object> listenerInstance = mock(Instance.class);
        when(lookup.select(Object.class, NamedLiteral.of("nonExistentListener")))
                .thenReturn(listenerInstance);
        when(listenerInstance.isResolvable()).thenReturn(false);

        Object service = CommonAIServiceCreator.create(lookup, MyAIServiceWithUnresolvableListener.class);
        assertNotNull(service);
    }

    // --- @OnThinking tests ---

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService
    interface ServiceWithOnThinking {
        String chat(String question);

        @OnThinking
        static void onThinking(ThinkingEmitted event) {}
    }

    @Test
    void create_wiresOnThinkingHandler() {
        Instance<Object> lookup = prepareLookups();
        Object service = CommonAIServiceCreator.create(lookup, ServiceWithOnThinking.class);
        assertNotNull(service);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService
    interface ServiceWithNonStaticOnThinking {
        String chat(String question);

        @OnThinking
        default void onThinking(ThinkingEmitted event) {}
    }

    @Test
    void create_throwsWhenOnThinkingMethodNotStatic() {
        Instance<Object> lookup = prepareLookups();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CommonAIServiceCreator.create(lookup, ServiceWithNonStaticOnThinking.class));
        assertTrue(ex.getMessage().contains("must be static"));
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService
    interface ServiceWithNonVoidOnThinking {
        String chat(String question);

        @OnThinking
        static String onThinking(ThinkingEmitted event) {
            return "";
        }
    }

    @Test
    void create_throwsWhenOnThinkingMethodNotVoid() {
        Instance<Object> lookup = prepareLookups();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CommonAIServiceCreator.create(lookup, ServiceWithNonVoidOnThinking.class));
        assertTrue(ex.getMessage().contains("must return void"));
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService
    interface ServiceWithWrongParamOnThinking {
        String chat(String question);

        @OnThinking
        static void onThinking(String wrongType) {}
    }

    @Test
    void create_throwsWhenOnThinkingMethodHasWrongParam() {
        Instance<Object> lookup = prepareLookups();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CommonAIServiceCreator.create(lookup, ServiceWithWrongParamOnThinking.class));
        assertTrue(ex.getMessage().contains("ThinkingEmitted"));
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService
    interface ServiceWithMultipleOnThinking {
        String chat(String question);

        @OnThinking
        static void handler1(ThinkingEmitted event) {}

        @OnThinking
        static void handler2(ThinkingEmitted event) {}
    }

    @Test
    void create_throwsWhenMultipleOnThinkingMethods() {
        Instance<Object> lookup = prepareLookups();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CommonAIServiceCreator.create(lookup, ServiceWithMultipleOnThinking.class));
        assertTrue(ex.getMessage().contains("Only one @OnThinking"));
    }

    // --- @OnThinking invocation tests ---

    static final AtomicReference<String> CAPTURED_THINKING = new AtomicReference<>();

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService
    interface ServiceWithInvocableOnThinking {
        String chat(String question);

        @OnThinking
        static void onThinking(ThinkingEmitted event) {
            CAPTURED_THINKING.set(event.text());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_onThinkingHandlerIsInvokedWhenThinkingPresent() {
        Instance<Object> lookup = prepareLookups();

        ChatModel thinkingModel = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest chatRequest) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.builder()
                                .text("result")
                                .thinking("deep thought")
                                .build())
                        .tokenUsage(new TokenUsage(10))
                        .build();
            }
        };

        Instance<ChatModel> cmInstance = mock(Instance.class);
        when(cmInstance.isResolvable()).thenReturn(true);
        when(cmInstance.get()).thenReturn(thinkingModel);
        when(lookup.select(ChatModel.class)).thenReturn(cmInstance);

        ServiceWithInvocableOnThinking service =
                CommonAIServiceCreator.create(lookup, ServiceWithInvocableOnThinking.class);
        service.chat("hello");

        assertEquals("deep thought", CAPTURED_THINKING.get());
    }

    static final AtomicReference<Boolean> THROWING_HANDLER_CALLED = new AtomicReference<>(false);

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @RegisterAIService
    interface ServiceWithThrowingOnThinking {
        String chat(String question);

        @OnThinking
        static void onThinking(ThinkingEmitted event) {
            THROWING_HANDLER_CALLED.set(true);
            throw new RuntimeException("handler explosion");
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void create_onThinkingHandlerExceptionIsSwallowed() {
        Instance<Object> lookup = prepareLookups();

        ChatModel thinkingModel = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest chatRequest) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.builder()
                                .text("result")
                                .thinking("some thinking")
                                .build())
                        .tokenUsage(new TokenUsage(10))
                        .build();
            }
        };

        Instance<ChatModel> cmInstance = mock(Instance.class);
        when(cmInstance.isResolvable()).thenReturn(true);
        when(cmInstance.get()).thenReturn(thinkingModel);
        when(lookup.select(ChatModel.class)).thenReturn(cmInstance);

        Logger logger = Logger.getLogger(CommonAIServiceCreator.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        logger.addHandler(handler);
        try {
            ServiceWithThrowingOnThinking service =
                    CommonAIServiceCreator.create(lookup, ServiceWithThrowingOnThinking.class);
            String result = service.chat("hello");

            assertNotNull(result);
            assertTrue(THROWING_HANDLER_CALLED.get());
            assertEquals(
                    1,
                    records.stream().filter(r -> r.getLevel() == Level.WARNING).count(),
                    "A single WARNING should be logged when the @OnThinking handler throws");
            LogRecord warning = records.stream()
                    .filter(r -> r.getLevel() == Level.WARNING)
                    .findFirst()
                    .orElseThrow();
            assertTrue(warning.getMessage().contains("handler explosion"));
            assertNull(warning.getThrown(), "Stack trace should not be attached for InvocationTargetException");
        } finally {
            logger.removeHandler(handler);
        }
    }
}
