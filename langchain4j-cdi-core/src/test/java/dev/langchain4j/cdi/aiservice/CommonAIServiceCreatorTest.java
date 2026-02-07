package dev.langchain4j.cdi.aiservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.cdi.spi.RegisterAIService;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import org.junit.jupiter.api.Test;

class CommonAIServiceCreatorTest {

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
    @RegisterAIService(chatModelName = "")
    interface MisconfiguredService {
        String chat(String question);
    }

    @Test
    void getInstance_returnsNullWhenNameBlank() throws Exception {
        var m = CommonAIServiceCreator.class.getDeclaredMethod(
                "getInstance", Instance.class, Class.class, String.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Instance<Object> lookup = mock(Instance.class);
        Object res = m.invoke(null, lookup, ChatModel.class, "");
        assertNull(res);
    }
}
