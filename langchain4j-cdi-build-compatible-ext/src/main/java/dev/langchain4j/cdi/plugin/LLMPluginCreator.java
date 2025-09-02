package dev.langchain4j.cdi.plugin;

import dev.langchain4j.cdi.core.config.spi.LLMConfigProvider;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;

public class LLMPluginCreator implements SyntheticBeanCreator<Object> {
    @Override
    public Object create(Instance<Object> lookup, Parameters params) {
        return CommonLLMPluginCreator.create(
                lookup,
                LLMConfigProvider.getLlmConfig(),
                params.get(LangChain4JPluginsBuildCompatibleExtension.PARAM_BEANNAME, String.class),
                params.get(LangChain4JPluginsBuildCompatibleExtension.PARAM_TARGET_CLASS, Class.class),
                params.get(LangChain4JPluginsBuildCompatibleExtension.PARAM_BUILDER_CLASS, Class.class));
    }
}
