package dev.langchain4j.cdi.plugin;

import dev.langchain4j.cdi.core.config.spi.LLMConfig;
import dev.langchain4j.cdi.core.config.spi.LLMConfigProvider;
import dev.langchain4j.cdi.spi.AISyntheticBeanCreatorClassProvider;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanBuilder;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.literal.NamedLiteral;
import java.util.logging.Logger;

/** Build-compatible CDI extension that registers LangChain4j plugin beans as synthetic beans at build time. */
public class LangChain4JPluginsBuildCompatibleExtension implements BuildCompatibleExtension {

    /** Logger for this extension. */
    public static final Logger LOGGER = Logger.getLogger(LangChain4JPluginsBuildCompatibleExtension.class.getName());

    /** Synthetic bean parameter key for the bean name. */
    public static final String PARAM_BEANNAME = "beanName";

    /** Synthetic bean parameter key for the target class. */
    public static final String PARAM_TARGET_CLASS = "targetClass";

    /** Synthetic bean parameter key for the builder class. */
    public static final String PARAM_BUILDER_CLASS = "builderClass";

    private LLMConfig llmConfig;

    /** Creates a new LangChain4JPluginsBuildCompatibleExtension. */
    public LangChain4JPluginsBuildCompatibleExtension() {}

    /**
     * Synthesis callback that creates synthetic CDI beans for all configured LLM plugins.
     *
     * @param syntheticComponents the synthetic components registry
     * @throws ClassNotFoundException if a configured plugin class cannot be found
     */
    @SuppressWarnings({"unused", "unchecked"})
    @Synthesis
    public void createSynthetics(SyntheticComponents syntheticComponents) throws ClassNotFoundException {
        if (llmConfig == null) {
            llmConfig = LLMConfigProvider.getLlmConfig();
        }
        LOGGER.info("CDI BCE Langchain4j plugin");

        CommonLLMPluginCreator.prepareAllLLMBeans(llmConfig, beanData -> {
            SyntheticBeanBuilder<Object> builder =
                    (SyntheticBeanBuilder<Object>) syntheticComponents.addBean(beanData.targetClass());

            builder.createWith(AISyntheticBeanCreatorClassProvider.getSyntheticBeanCreatorClass())
                    .type(beanData.targetClass())
                    .scope(beanData.scopeClass())
                    .name(beanData.beanName())
                    .qualifier(NamedLiteral.of(beanData.beanName()))
                    .withParam(PARAM_BEANNAME, beanData.beanName())
                    .withParam(PARAM_TARGET_CLASS, beanData.targetClass())
                    .withParam(PARAM_BUILDER_CLASS, beanData.builderClass());

            for (Class<?> newInterface : beanData.targetClass().getInterfaces()) builder.type(newInterface);
        });
    }
}
