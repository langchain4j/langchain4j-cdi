package dev.langchain4j.cdi.aiservice;

import dev.langchain4j.agentic.internal.AgenticScopeOwner;
import dev.langchain4j.agentic.internal.InternalAgent;
import dev.langchain4j.agentic.scope.AgenticScopeAccess;
import dev.langchain4j.cdi.agent.AgentAnnotationMeta;
import dev.langchain4j.cdi.agent.CommonAgentCreator;
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
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.FieldConfig;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanBuilder;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.FieldInfo;
import jakarta.enterprise.lang.model.types.ClassType;
import jakarta.inject.Named;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Build-compatible CDI extension that discovers {@code @RegisterAIService} and agent-annotated interfaces at build time
 * and registers synthetic beans for them.
 */
public class Langchain4JAIServiceBuildCompatibleExtension implements BuildCompatibleExtension {
    private static final Logger LOGGER = Logger.getLogger(Langchain4JAIServiceBuildCompatibleExtension.class.getName());
    private static final Set<Class<?>> detectedAIServicesDeclaredInterfaces = new HashSet<>();
    private static final Set<Class<?>> detectedAgentDeclaredInterfaces = new HashSet<>();
    private static final Set<String> detectedTools = new HashSet<>();

    /** Synthetic bean parameter key for the AI service interface class. */
    public static final String PARAM_INTERFACE_CLASS = "interfaceClass";

    /** Synthetic bean parameter key for the agent interface class. */
    public static final String PARAM_AGENT_INTERFACE_CLASS = "agentInterfaceClass";

    /** Creates a new {@code Langchain4JAIServiceBuildCompatibleExtension}. */
    public Langchain4JAIServiceBuildCompatibleExtension() {}

    /**
     * Returns the set of detected AI service interface classes.
     *
     * @return the detected AI service interfaces
     */
    public static Set<Class<?>> getDetectedAIServicesDeclaredInterfaces() {
        return detectedAIServicesDeclaredInterfaces;
    }

    /**
     * Returns the set of detected agent interface classes.
     *
     * @return the detected agent interfaces
     */
    public static Set<Class<?>> getDetectedAgentDeclaredInterfaces() {
        return detectedAgentDeclaredInterfaces;
    }

    /**
     * Adds a {@link Named} qualifier to detected tool classes so they are not removed by Quarkus bean pruning.
     *
     * @param classConfig the class configuration to enhance
     * @throws ClassNotFoundException if the tool class cannot be loaded
     */
    @SuppressWarnings("unused")
    @Enhancement(types = Object.class, withSubtypes = true)
    @Priority(20)
    public void protectedToolsToBePurgedByQuarkus(ClassConfig classConfig) throws ClassNotFoundException {
        if (detectedTools.contains(classConfig.info().name())) {
            Class<?> toolClass = getLoadClass(classConfig.info().name());
            if (toolClass.getAnnotation(Named.class) == null) {
                classConfig.addAnnotation(NamedLiteral.of(
                        "quarkus-protected-" + classConfig.info().name()));
                LOGGER.info("Add a Name to " + classConfig.info().name());
            }
        }
    }

    /**
     * Detects classes annotated with {@link RegisterAIService} and registers them for synthesis.
     *
     * @param classConfig the class configuration to inspect
     * @throws ClassNotFoundException if the class cannot be loaded
     */
    @SuppressWarnings("unused")
    @Enhancement(types = Object.class, withAnnotations = RegisterAIService.class, withSubtypes = true)
    @Priority(10)
    public void detectRegisterAIService(ClassConfig classConfig) throws ClassNotFoundException {
        ClassInfo classInfo = classConfig.info();
        registerAIService(classInfo);
    }

    /**
     * Detects fields whose type is annotated with {@link RegisterAIService} or an agent annotation and registers the
     * corresponding interface for synthesis.
     *
     * @param config the field configuration to inspect
     * @throws ClassNotFoundException if the field type class cannot be loaded
     */
    @Enhancement(types = Object.class, withSubtypes = true)
    @Priority(30)
    public void detectRegisterAIService(FieldConfig config) throws ClassNotFoundException {
        FieldInfo info = config.info();
        if (info.type().isClass()) {
            ClassType classType = info.type().asClass();
            ClassInfo classInfo = classType.declaration();
            if (classInfo.name().equals(Object.class.getName())) {
                return;
            }
            LOGGER.fine("Detecting RegisterAIService on type " + classInfo.name());
            AnnotationInfo annotationInfo = classInfo.annotation(RegisterAIService.class);
            if (annotationInfo != null) {
                registerAIService(classInfo);
            }
            if (hasAnyAgentAnnotation(classInfo)) {
                registerAgent(classInfo);
            }
        }
    }

    /**
     * Detects classes annotated with any of the 11 agent topology annotations and registers them for synthesis.
     *
     * @param classConfig the class configuration to inspect
     * @throws ClassNotFoundException if the class cannot be loaded
     */
    @SuppressWarnings("unused")
    @Enhancement(
            types = Object.class,
            withAnnotations = {
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
            },
            withSubtypes = true)
    @Priority(11)
    public void detectRegisterAgent(ClassConfig classConfig) throws ClassNotFoundException {
        ClassInfo classInfo = classConfig.info();
        registerAgent(classInfo);
    }

    private void registerAIService(ClassInfo classInfo) throws ClassNotFoundException {
        if (classInfo.isInterface()) {
            String className = classInfo.name();
            Class<?> interfaceClass = getLoadClass(className);
            if (!detectedAIServicesDeclaredInterfaces.contains(interfaceClass)) {
                LOGGER.info("RegisterAIService of type " + classInfo.name());
                detectedAIServicesDeclaredInterfaces.add(interfaceClass);
            }

            RegisterAIService annotation = interfaceClass.getAnnotation(RegisterAIService.class);
            detectedTools.addAll(
                    Arrays.stream(annotation.tools()).map(Class::getName).collect(Collectors.toList()));
        } else {
            LOGGER.warning(
                    "The class is Annotated with @RegisterAIService, but only interface are allowed" + classInfo);
        }
    }

    private void registerAgent(ClassInfo classInfo) throws ClassNotFoundException {
        if (classInfo.isInterface()) {
            String className = classInfo.name();
            Class<?> interfaceClass = getLoadClass(className);
            if (!detectedAgentDeclaredInterfaces.contains(interfaceClass)) {
                LOGGER.info("RegisterAgent of type " + classInfo.name());
                detectedAgentDeclaredInterfaces.add(interfaceClass);
            }
        } else {
            LOGGER.warning(
                    "The class is Annotated with an agent annotation, but only interfaces are allowed: " + classInfo);
        }
    }

    // Mirrors AgentAnnotationMeta.detect(Class<?>) but operates on the build-time ClassInfo type
    // rather than a runtime Class. Keep both lists in sync when adding new topology annotations.
    private static boolean hasAnyAgentAnnotation(ClassInfo classInfo) {
        return classInfo.annotation(RegisterSimpleAgent.class) != null
                || classInfo.annotation(RegisterSequenceAgent.class) != null
                || classInfo.annotation(RegisterLoopAgent.class) != null
                || classInfo.annotation(RegisterParallelAgent.class) != null
                || classInfo.annotation(RegisterParallelMapperAgent.class) != null
                || classInfo.annotation(RegisterConditionalAgent.class) != null
                || classInfo.annotation(RegisterSupervisorAgent.class) != null
                || classInfo.annotation(RegisterPlannerAgent.class) != null
                || classInfo.annotation(RegisterA2AAgent.class) != null
                || classInfo.annotation(RegisterMcpClientAgent.class) != null
                || classInfo.annotation(RegisterHumanInTheLoopAgent.class) != null;
    }

    private static Class<?> getLoadClass(String className) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(className);
    }

    /**
     * Synthesizes CDI beans for all detected AI service and agent interfaces.
     *
     * @param syntheticComponents the synthetic components builder provided by the CDI container
     * @throws ClassNotFoundException if a required class cannot be loaded
     */
    @SuppressWarnings({"unused", "unchecked"})
    @Synthesis
    public void synthesisAllRegisterAIServices(SyntheticComponents syntheticComponents) throws ClassNotFoundException {
        LOGGER.info("synthesisAllRegisterAIServices");

        for (Class<?> interfaceClass : detectedAIServicesDeclaredInterfaces) {
            LOGGER.info("Create synthetic AI service " + interfaceClass);
            RegisterAIService annotation = interfaceClass.getAnnotation(RegisterAIService.class);

            SyntheticBeanBuilder<Object> builder =
                    (SyntheticBeanBuilder<Object>) syntheticComponents.addBean(interfaceClass);

            builder.createWith(AIServiceCreator.class)
                    .type(interfaceClass)
                    .scope(annotation.scope())
                    .name("registeredAIService-" + interfaceClass.getName())
                    .withParam(PARAM_INTERFACE_CLASS, interfaceClass);
        }

        for (Class<?> interfaceClass : detectedAgentDeclaredInterfaces) {
            LOGGER.info("Create synthetic agent " + interfaceClass);
            AgentAnnotationMeta meta = AgentAnnotationMeta.detect(interfaceClass);

            SyntheticBeanBuilder<Object> builder =
                    (SyntheticBeanBuilder<Object>) syntheticComponents.addBean(interfaceClass);

            String agentName = meta != null ? CdiLookupHelper.resolveExpression(meta.rawName()) : null;
            String beanName = (agentName != null && !agentName.isBlank())
                    ? agentName
                    : CommonAgentCreator.AGENT_BEAN_NAME_PREFIX + interfaceClass.getName();
            Class<? extends java.lang.annotation.Annotation> scope =
                    meta != null ? meta.scope() : jakarta.enterprise.context.ApplicationScoped.class;

            SyntheticBeanBuilder<Object> agentBuilder = builder.createWith(AIAgentCreator.class)
                    .type(interfaceClass)
                    .type(InternalAgent.class)
                    .type(AgenticScopeOwner.class)
                    .type(AgenticScopeAccess.class);
            if (meta != null && meta.annotationClass() == RegisterHumanInTheLoopAgent.class) {
                agentBuilder.type(CommonAgentCreator.HumanInTheLoopHolder.class);
            }
            agentBuilder
                    .type(Object.class)
                    .scope(scope)
                    .name(beanName)
                    .withParam(PARAM_AGENT_INTERFACE_CLASS, interfaceClass);
        }
    }
}
