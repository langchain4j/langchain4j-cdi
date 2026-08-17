package dev.langchain4j.cdi.aiservice;

import dev.langchain4j.cdi.spi.RegisterAIService;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.observability.api.listener.AiServiceResponseReceivedListener;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.enterprise.inject.Instance;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility to build LangChain4j AiServices proxies from CDI beans and the @RegisterAIService metadata.
 *
 * <p>The method create() inspects the provided service interface for @RegisterAIService and tries to resolve optional
 * collaborating beans from the CDI container (by name or default): - ChatModel or StreamingChatModel - ContentRetriever
 * or RetrievalAugmentor (RetrievalAugmentor has priority) - ToolProvider and tools[] classes declared in the annotation
 * (both are applied when present; avoid overlapping tool names as LangChain4j will throw at runtime) - ChatMemory or
 * ChatMemoryProvider - ModerationModel - InputGuardrails and OutputGuardrails (by class or named CDI beans)
 *
 * <p>Only the components that are resolvable are wired into the AiServices builder.
 */
public class CommonAIServiceCreator {

    /** Utility class — not instantiable. */
    private CommonAIServiceCreator() {}

    private static final Logger LOGGER = Logger.getLogger(CommonAIServiceCreator.class.getName());

    /**
     * Create a LangChain4j AI service proxy for the given annotated interface.
     *
     * @param <X> the AI service interface type
     * @param lookup CDI Instance used to resolve named beans (models, tools, memories, etc.).
     * @param interfaceClass the AI service interface annotated with {@link dev.langchain4j.cdi.spi.RegisterAIService}.
     * @return a runtime proxy implementing the given interface.
     */
    public static <X> X create(Instance<Object> lookup, Class<X> interfaceClass) {
        RegisterAIService annotation = interfaceClass.getAnnotation(RegisterAIService.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "Interface " + interfaceClass.getName() + " must be annotated with @RegisterAIService");
        }
        String chatModelName = annotation.chatModelName();
        String streamingChatModelName = annotation.streamingChatModelName();
        // Instances
        Instance<ChatModel> chatModelInstance = resolveChatModel(lookup, chatModelName, streamingChatModelName);
        Instance<StreamingChatModel> streamingChatModel =
                CdiLookupHelper.getInstance(lookup, StreamingChatModel.class, streamingChatModelName);
        Instance<ContentRetriever> contentRetriever =
                CdiLookupHelper.getInstance(lookup, ContentRetriever.class, annotation.contentRetrieverName());
        Instance<RetrievalAugmentor> retrievalAugmentor =
                CdiLookupHelper.getInstance(lookup, RetrievalAugmentor.class, annotation.retrievalAugmentorName());
        Instance<ToolProvider> toolProvider =
                CdiLookupHelper.getInstance(lookup, ToolProvider.class, annotation.toolProviderName());

        AiServices<X> builder = AiServices.builder(interfaceClass);
        if (chatModelInstance != null && chatModelInstance.isResolvable()) {
            LOGGER.fine("ChatModel " + chatModelInstance.get());
            builder.chatModel(chatModelInstance.get());
        }
        if (streamingChatModel != null && streamingChatModel.isResolvable()) {
            LOGGER.fine("StreamingChatModel " + streamingChatModel.get());
            builder.streamingChatModel(streamingChatModel.get());
        }
        // AiServices requires only one of [retriever, contentRetriever, retrievalAugmentor].
        // If a RetrievalAugmentor is provided, prefer it and do not set ContentRetriever.
        if (retrievalAugmentor != null && retrievalAugmentor.isResolvable()) {
            LOGGER.fine("RetrievalAugmentor " + retrievalAugmentor.get());
            builder.retrievalAugmentor(retrievalAugmentor.get());
        } else if (contentRetriever != null && contentRetriever.isResolvable()) {
            LOGGER.fine("ContentRetriever " + contentRetriever.get());
            builder.contentRetriever(contentRetriever.get());
        }
        if (toolProvider != null && toolProvider.isResolvable()) {
            LOGGER.fine("ToolProvider " + toolProvider.get());
            builder.toolProvider(toolProvider.get());
        }
        if (annotation.tools().length > 0) {
            List<Object> tools = CdiLookupHelper.resolveToolInstances(annotation.tools(), lookup);
            if (toolProvider != null && toolProvider.isResolvable()) {
                LOGGER.warning("Both toolProviderName and tools[] are configured on "
                        + interfaceClass.getSimpleName()
                        + "; overlapping tool names will cause IllegalConfigurationException at runtime.");
            }
            LOGGER.fine("Tools " + tools);
            builder.tools(tools);
        }
        Instance<ChatMemory> chatMemory =
                CdiLookupHelper.getInstance(lookup, ChatMemory.class, annotation.chatMemoryName());
        if (chatMemory != null && chatMemory.isResolvable()) {
            ChatMemory chatMemoryInstance = chatMemory.get();
            LOGGER.fine("ChatMemory " + chatMemoryInstance);
            builder.chatMemory(chatMemoryInstance);
        }

        Instance<ChatMemoryProvider> chatMemoryProvider =
                CdiLookupHelper.getInstance(lookup, ChatMemoryProvider.class, annotation.chatMemoryProviderName());
        if (chatMemoryProvider != null && chatMemoryProvider.isResolvable()) {
            LOGGER.fine("ChatMemoryProvider " + chatMemoryProvider.get());
            builder.chatMemoryProvider(chatMemoryProvider.get());
        }

        Instance<ModerationModel> moderationModelInstance =
                CdiLookupHelper.getInstance(lookup, ModerationModel.class, annotation.moderationModelName());
        if (moderationModelInstance != null && moderationModelInstance.isResolvable()) {
            LOGGER.fine("ModerationModel " + moderationModelInstance.get());
            builder.moderationModel(moderationModelInstance.get());
        }
        List<InputGuardrail> inputGuardrails = CdiLookupHelper.resolveInputGuardrails(
                lookup, annotation.inputGuardrails(), annotation.inputGuardrailNames(), interfaceClass.getSimpleName());
        if (!inputGuardrails.isEmpty()) {
            LOGGER.fine("InputGuardrails " + inputGuardrails);
            builder.inputGuardrails(inputGuardrails);
        }
        List<OutputGuardrail> outputGuardrails = CdiLookupHelper.resolveOutputGuardrails(
                lookup,
                annotation.outputGuardrails(),
                annotation.outputGuardrailNames(),
                interfaceClass.getSimpleName());
        if (!outputGuardrails.isEmpty()) {
            LOGGER.fine("OutputGuardrails " + outputGuardrails);
            builder.outputGuardrails(outputGuardrails);
        }
        registerNamedListeners(builder, lookup, annotation.listenerNames(), interfaceClass);
        registerThinkingHandler(builder, interfaceClass);
        return builder.build();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerNamedListeners(
            AiServices<?> builder, Instance<Object> lookup, String[] listenerNames, Class<?> interfaceClass) {
        for (String name : listenerNames) {
            if (!CdiLookupHelper.hasText(name)) {
                continue;
            }
            Object resolved = CdiLookupHelper.resolveSingle(lookup, Object.class, name);
            if (resolved instanceof AiServiceListener listener) {
                LOGGER.fine(() -> "AiServiceListener '" + name + "' on " + interfaceClass.getSimpleName());
                builder.registerListener(listener);
            } else if (resolved != null) {
                LOGGER.warning("Bean '" + name + "' on " + interfaceClass.getSimpleName()
                        + " does not implement AiServiceListener — skipped");
            } else {
                LOGGER.warning("AiServiceListener '" + name + "' not resolvable for " + interfaceClass.getSimpleName()
                        + " — skipped");
            }
        }
    }

    private static void registerThinkingHandler(AiServices<?> builder, Class<?> interfaceClass) {
        Method handler = findOnThinkingMethod(interfaceClass);
        if (handler == null) {
            return;
        }
        handler.setAccessible(true);
        Method thinkingMethod = handler;
        AiServiceResponseReceivedListener listener = event -> {
            String thinking = event.response().aiMessage().thinking();
            if (thinking == null || thinking.isBlank()) {
                return;
            }
            try {
                var ctx = event.invocationContext();
                ThinkingEmitted emitted = new DefaultThinkingEmitted(
                        thinking,
                        ctx != null ? ctx.methodName() : null,
                        interfaceClass,
                        ctx != null ? ctx.chatMemoryId() : null,
                        Instant.now());
                thinkingMethod.invoke(null, emitted);
            } catch (InvocationTargetException e) {
                LOGGER.log(
                        Level.WARNING,
                        "@OnThinking handler on " + interfaceClass.getSimpleName() + " failed: "
                                + e.getCause().getMessage());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "@OnThinking handler on " + interfaceClass.getSimpleName() + " failed", e);
            }
        };
        LOGGER.fine(() -> "@OnThinking handler on " + interfaceClass.getSimpleName());
        builder.registerListener(listener);
    }

    private static Method findOnThinkingMethod(Class<?> interfaceClass) {
        Method handler = null;
        for (Method m : interfaceClass.getDeclaredMethods()) {
            if (m.getAnnotation(OnThinking.class) == null) {
                continue;
            }
            if (handler != null) {
                throw new IllegalArgumentException("Only one @OnThinking method is allowed on "
                        + interfaceClass.getName() + " but found both " + handler.getName() + " and " + m.getName());
            }
            if (!Modifier.isStatic(m.getModifiers())) {
                throw new IllegalArgumentException(
                        "@OnThinking method " + interfaceClass.getName() + "." + m.getName() + " must be static");
            }
            if (m.getReturnType() != void.class) {
                throw new IllegalArgumentException(
                        "@OnThinking method " + interfaceClass.getName() + "." + m.getName() + " must return void");
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length != 1 || !ThinkingEmitted.class.isAssignableFrom(params[0])) {
                throw new IllegalArgumentException("@OnThinking method " + interfaceClass.getName() + "." + m.getName()
                        + " must accept a single ThinkingEmitted parameter");
            }
            handler = m;
        }
        return handler;
    }

    /**
     * Resolve ChatModel with fallback to default if no named instance and no streaming model configured. If a named
     * ChatModel is not resolvable and no StreamingChatModel is configured, try to resolve the default ChatModel to
     * satisfy AiServices requirement.
     */
    private static Instance<ChatModel> resolveChatModel(
            Instance<Object> lookup, String chatModelName, String streamingChatModelName) {
        Instance<ChatModel> chatModelInstance = CdiLookupHelper.getInstance(lookup, ChatModel.class, chatModelName);

        // If neither ChatModel nor StreamingChatModel is configured, try default ChatModel
        if ((chatModelInstance == null || !chatModelInstance.isResolvable())
                && !CdiLookupHelper.hasText(streamingChatModelName)) {
            return lookup.select(ChatModel.class);
        }

        return chatModelInstance;
    }
}
