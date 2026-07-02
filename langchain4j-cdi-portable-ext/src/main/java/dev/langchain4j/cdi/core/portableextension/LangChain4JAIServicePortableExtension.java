package dev.langchain4j.cdi.core.portableextension;

import dev.langchain4j.cdi.agent.AgentAnnotationMeta;
import dev.langchain4j.cdi.spi.RegisterA2AAgent;
import dev.langchain4j.cdi.spi.RegisterAIService;
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
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.enterprise.inject.spi.WithAnnotations;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * CDI portable extension that discovers interfaces annotated with {@link RegisterAIService} and agent topology
 * stereotypes, then registers synthetic beans for each at deployment time.
 */
public class LangChain4JAIServicePortableExtension implements Extension {
    private static final Logger LOGGER = Logger.getLogger(LangChain4JAIServicePortableExtension.class.getName());
    private static final Set<Class<?>> detectedAIServicesDeclaredInterfaces = new HashSet<>();
    private static final Set<Class<?>> detectedAgentDeclaredInterfaces = new HashSet<>();

    /** Creates a new instance of this portable extension. */
    public LangChain4JAIServicePortableExtension() {}

    /**
     * Returns the set of AI service interfaces detected during bean discovery.
     *
     * @return the detected AI service interfaces
     */
    public static Set<Class<?>> getDetectedAIServicesDeclaredInterfaces() {
        return detectedAIServicesDeclaredInterfaces;
    }

    /**
     * Returns the set of agent interfaces detected during bean discovery.
     *
     * @return the detected agent interfaces
     */
    public static Set<Class<?>> getDetectedAgentDeclaredInterfaces() {
        return detectedAgentDeclaredInterfaces;
    }

    // These sets are static so they persist across container restarts in the same JVM (e.g. Arquillian,
    // Weld SE restart). The BeforeBeanDiscovery observer below clears them at the start of each new
    // container lifecycle to prevent stale entries from a previous run from leaking into the new one.
    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event) {
        detectedAIServicesDeclaredInterfaces.clear();
        detectedAgentDeclaredInterfaces.clear();
    }

    <T> void processAnnotatedType(@Observes @WithAnnotations({RegisterAIService.class}) ProcessAnnotatedType<T> pat) {
        if (pat.getAnnotatedType().getJavaClass().isInterface()) {
            LOGGER.info("processAnnotatedType register "
                    + pat.getAnnotatedType().getJavaClass().getName());
            detectedAIServicesDeclaredInterfaces.add(pat.getAnnotatedType().getJavaClass());
        } else {
            LOGGER.warning("processAnnotatedType reject "
                    + pat.getAnnotatedType().getJavaClass().getName() + " which is not an interface");
            pat.veto();
        }
    }

    <T> void processAgentAnnotatedType(
            @Observes
                    @WithAnnotations({
                        RegisterSimpleAgent.class,
                        RegisterSequenceAgent.class,
                        RegisterLoopAgent.class,
                        RegisterParallelAgent.class,
                        RegisterParallelMapperAgent.class,
                        RegisterConditionalAgent.class,
                        RegisterSupervisorAgent.class,
                        RegisterPlannerAgent.class,
                        RegisterA2AAgent.class,
                        RegisterMcpClientAgent.class,
                        RegisterHumanInTheLoopAgent.class
                    })
                    ProcessAnnotatedType<T> pat) {
        if (pat.getAnnotatedType().getJavaClass().isInterface()) {
            LOGGER.info("processAnnotatedType register agent "
                    + pat.getAnnotatedType().getJavaClass().getName());
            detectedAgentDeclaredInterfaces.add(pat.getAnnotatedType().getJavaClass());
        } else {
            LOGGER.warning("processAnnotatedType reject "
                    + pat.getAnnotatedType().getJavaClass().getName() + " which is not an interface");
            pat.veto();
        }
    }

    /**
     * This is useful for application servers that can't support proccessAnnotatedType.
     *
     * @param event
     */
    void processInjectionPoints(@Observes ProcessInjectionPoint<?, ?> event) {
        if (event.getInjectionPoint().getBean() == null) {
            Class<?> rawType = Reflections.getRawType(event.getInjectionPoint().getType());
            if (classSatisfies(rawType, RegisterAIService.class)) detectedAIServicesDeclaredInterfaces.add(rawType);
            if (AgentAnnotationMeta.isAgentInterface(rawType)) detectedAgentDeclaredInterfaces.add(rawType);
        }

        if (Instance.class.equals(
                Reflections.getRawType(event.getInjectionPoint().getType()))) {
            Class<?> parameterizedType = Reflections.getRawType(getFacadeType(event.getInjectionPoint()));
            if (classSatisfies(parameterizedType, RegisterAIService.class))
                detectedAIServicesDeclaredInterfaces.add(parameterizedType);
            if (AgentAnnotationMeta.isAgentInterface(parameterizedType))
                detectedAgentDeclaredInterfaces.add(parameterizedType);
        }
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager)
            throws ClassNotFoundException {
        for (Class<?> aiServiceClass : detectedAIServicesDeclaredInterfaces) {
            LOGGER.info("afterBeanDiscovery create synthetic AI service:  " + aiServiceClass.getName());
            afterBeanDiscovery.addBean(new LangChain4JAIServiceBean<>(aiServiceClass, beanManager));
        }
        for (Class<?> agentClass : detectedAgentDeclaredInterfaces) {
            LOGGER.info("afterBeanDiscovery create synthetic agent:  " + agentClass.getName());
            afterBeanDiscovery.addBean(new LangChain4JAIAgentBean<>(agentClass, beanManager));
        }
    }

    private <T extends Annotation> boolean classSatisfies(Class<?> clazz, Class<T> annotationClass) {
        if (!clazz.isInterface()) return false;
        T annotation = clazz.getAnnotation(annotationClass);
        return (annotation != null);
    }

    private Type getFacadeType(InjectionPoint injectionPoint) {
        Type genericType = injectionPoint.getType();
        if (genericType instanceof ParameterizedType) {
            return ((ParameterizedType) genericType).getActualTypeArguments()[0];
        }
        return null;
    }
}
