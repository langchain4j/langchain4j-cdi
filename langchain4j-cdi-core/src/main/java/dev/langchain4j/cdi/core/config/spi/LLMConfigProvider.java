package dev.langchain4j.cdi.core.config.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Bootstrap and access point for the LLMConfig implementation.
 * <p>
 * It discovers an implementation via ServiceLoader and initializes it lazily on first access.
 */
public class LLMConfigProvider {

    private static LLMConfig llmConfig;
    private static volatile boolean initialized = false;
    private static final Logger LOGGER = Logger.getLogger(LLMConfigProvider.class.getName());

    static {
        ServiceLoader<LLMConfig> loader = ServiceLoader.load(LLMConfig.class,
                Thread.currentThread().getContextClassLoader());
        final List<LLMConfig> factories = new ArrayList<>();
        loader.forEach(factories::add);
        if (factories.isEmpty()) {
            // Use current classloader to load the LLMConfig implementation as a fallback option
            loader = ServiceLoader.load(LLMConfig.class, LLMConfig.class.getClassLoader());
            loader.forEach(factories::add);
            if (factories.isEmpty()) {
                throw new RuntimeException("No service Found for LLMConfig interface");
            }
        }
        llmConfig = factories.iterator().next(); //loader.findFirst().orElse(null);
        LOGGER.fine("Found LLMConfig interface: " + llmConfig.getClass().getName());
    }

    public static LLMConfig getLlmConfig() {
        if (!initialized) {
            initialized = true;
            llmConfig.init();
        }

        return llmConfig;
    }
}
