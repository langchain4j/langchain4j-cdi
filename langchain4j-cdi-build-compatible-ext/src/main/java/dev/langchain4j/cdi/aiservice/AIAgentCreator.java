package dev.langchain4j.cdi.aiservice;

import dev.langchain4j.cdi.agent.CommonAgentCreator;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;

/**
 * Synthetic bean creator that instantiates agent proxies at runtime using the interface class stored during build-time
 * synthesis.
 */
public class AIAgentCreator implements SyntheticBeanCreator<Object> {

    /** Creates a new {@code AIAgentCreator}. */
    public AIAgentCreator() {}

    @Override
    public Object create(Instance<Object> lookup, Parameters params) {
        return CommonAgentCreator.create(
                lookup,
                params.get(Langchain4JAIServiceBuildCompatibleExtension.PARAM_AGENT_INTERFACE_CLASS, Class.class));
    }
}
