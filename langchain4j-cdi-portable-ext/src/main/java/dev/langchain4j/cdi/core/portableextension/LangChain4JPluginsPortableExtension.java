package dev.langchain4j.cdi.core.portableextension;

import dev.langchain4j.cdi.core.config.spi.LLMConfig;
import dev.langchain4j.cdi.core.config.spi.LLMConfigProvider;
import dev.langchain4j.cdi.plugin.CommonLLMPluginCreator;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * CDI portable extension that creates beans from LLM configuration properties discovered via the
 * {@link LLMConfigProvider} SPI.
 */
public class LangChain4JPluginsPortableExtension implements Extension {
    private static final Logger LOGGER = Logger.getLogger(LangChain4JPluginsPortableExtension.class.getName());

    /** Creates a new instance of this portable extension. */
    public LangChain4JPluginsPortableExtension() {}

    private LLMConfig llmConfig;

    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager)
            throws ClassNotFoundException {
        if (llmConfig == null) {
            llmConfig = LLMConfigProvider.getLlmConfig();
        }

        CommonLLMPluginCreator.prepareAllLLMBeans(llmConfig, beanData -> {
            LOGGER.fine("Add Bean " + beanData.targetClass() + " " + beanData.scopeClass() + " " + beanData.beanName());

            afterBeanDiscovery
                    .addBean()
                    .types(beanData.targetClass())
                    .addTypes(beanData.targetClass().getInterfaces())
                    .scope(beanData.scopeClass())
                    .name(beanData.beanName())
                    .qualifiers(NamedLiteral.of(beanData.beanName()))
                    .produceWith(beanData.callback());

            LOGGER.info("Types: " + beanData.targetClass() + ","
                    + Arrays.stream(beanData.targetClass().getInterfaces())
                            .map(Class::getName)
                            .collect(Collectors.joining(",")));
        });
    }
}
