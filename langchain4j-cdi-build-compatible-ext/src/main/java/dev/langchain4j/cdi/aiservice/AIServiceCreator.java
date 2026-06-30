package dev.langchain4j.cdi.aiservice;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;

/**
 * Synthetic bean creator that instantiates AI service proxies at runtime using the interface class stored during
 * build-time synthesis.
 */
public class AIServiceCreator implements SyntheticBeanCreator<Object> {

    /** Creates a new {@code AIServiceCreator}. */
    public AIServiceCreator() {}

    @Override
    public Object create(Instance<Object> lookup, Parameters params) {
        return CommonAIServiceCreator.create(
                lookup, params.get(Langchain4JAIServiceBuildCompatibleExtension.PARAM_INTERFACE_CLASS, Class.class));
    }
}
