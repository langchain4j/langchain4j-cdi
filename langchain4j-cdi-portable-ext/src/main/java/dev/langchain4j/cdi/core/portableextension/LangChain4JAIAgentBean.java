package dev.langchain4j.cdi.core.portableextension;

import dev.langchain4j.agentic.internal.AgenticScopeOwner;
import dev.langchain4j.agentic.internal.InternalAgent;
import dev.langchain4j.agentic.scope.AgenticScopeAccess;
import dev.langchain4j.cdi.agent.AgentAnnotationMeta;
import dev.langchain4j.cdi.agent.CommonAgentCreator;
import dev.langchain4j.cdi.aiservice.CdiLookupHelper;
import dev.langchain4j.cdi.spi.RegisterHumanInTheLoopAgent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LangChain4JAIAgentBean<T> implements Bean<T>, PassivationCapable {

    private final Class<T> agentInterfaceClass;
    private final BeanManager beanManager;
    private final Class<? extends Annotation> scope;
    private final String agentName;
    private final Class<? extends Annotation> stereotypeAnnotationClass;
    private Set<Annotation> interceptorBindings;

    public LangChain4JAIAgentBean(Class<T> agentInterfaceClass, BeanManager beanManager) {
        super();
        this.agentInterfaceClass = agentInterfaceClass;
        AgentAnnotationMeta meta = AgentAnnotationMeta.detect(agentInterfaceClass);
        this.scope = meta != null ? meta.scope() : jakarta.enterprise.context.ApplicationScoped.class;
        String resolvedName = meta != null ? CdiLookupHelper.resolveExpression(meta.rawName()) : null;
        this.agentName = (resolvedName != null && !resolvedName.isBlank()) ? resolvedName : null;
        this.beanManager = beanManager;
        this.stereotypeAnnotationClass =
                meta != null ? meta.annotationClass() : dev.langchain4j.cdi.spi.RegisterSimpleAgent.class;
    }

    @Override
    public String getId() {
        return agentInterfaceClass.getName();
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        T instance = CommonAgentCreator.create(CDI.current(), agentInterfaceClass);
        if (!getInterceptorBindings().isEmpty()) {
            InterceptionFactory<T> factory =
                    beanManager.createInterceptionFactory(creationalContext, agentInterfaceClass);
            interceptorBindings.stream().forEach(factory.configure()::add);
            instance = factory.createInterceptedInstance(instance);
        }
        return instance;
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        // Intentional no-op: the agent proxy is a JDK dynamic proxy with no closeable state of its
        // own. All collaborating resources (ChatModel, tools, memory, AgentListener, etc.) are
        // CDI-managed beans with their own scopes and destroy callbacks handled by the container.
    }

    @Override
    public Set<Type> getTypes() {
        Set<Type> types = new HashSet<>();
        types.add(agentInterfaceClass);
        types.add(InternalAgent.class);
        types.add(AgenticScopeOwner.class);
        types.add(AgenticScopeAccess.class);
        if (stereotypeAnnotationClass == RegisterHumanInTheLoopAgent.class) {
            types.add(CommonAgentCreator.HumanInTheLoopHolder.class);
        }
        types.add(Object.class);
        return Collections.unmodifiableSet(types);
    }

    @Override
    public Set<Annotation> getQualifiers() {
        Set<Annotation> annotations = new HashSet<>();
        annotations.add(new AnnotationLiteral<Default>() {});
        annotations.add(new AnnotationLiteral<Any>() {});
        annotations.add(NamedLiteral.of(getName()));
        return Collections.unmodifiableSet(annotations);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    @Override
    public String getName() {
        if (agentName != null) {
            return agentName;
        }
        return CommonAgentCreator.AGENT_BEAN_NAME_PREFIX + agentInterfaceClass.getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.singleton(stereotypeAnnotationClass);
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public Class<?> getBeanClass() {
        return agentInterfaceClass;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    public Set<Annotation> getInterceptorBindings() {
        if (interceptorBindings == null) interceptorBindings = new HashSet<>();
        return interceptorBindings;
    }

    @Override
    public String toString() {
        return "Agent [ interfaceType: " + agentInterfaceClass.getSimpleName() + " ] with Qualifiers ["
                + getQualifiers() + "]";
    }
}
