package dev.langchain4j.cdi.core.portableextension;

import dev.langchain4j.cdi.decision.CommonDecisionServiceCreator;
import dev.langchain4j.cdi.spi.RegisterDecisionService;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
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

/**
 * CDI {@link Bean} implementation that creates AI decision service proxies for interfaces annotated with
 * {@link RegisterDecisionService}.
 *
 * @param <T> the AI decision service interface type
 * @author Buhake Sindi
 * @since 08 October 2026
 */
public class LangChain4JDecisionServiceBean<T> implements Bean<T>, InterceptorBeanAttributes<T>, PassivationCapable {

    private final Class<T> decisionServiceInterfaceClass;

    private final BeanManager beanManager;

    private final Class<? extends Annotation> scope;

    private Set<Annotation> interceptorBindings;

    /**
     * Creates a new bean definition for the given AI service interface.
     *
     * @param aiServiceInterfaceClass the interface annotated with {@link RegisterDecisionService}
     * @param beanManager the CDI bean manager used for interception support
     */
    public LangChain4JDecisionServiceBean(Class<T> aiServiceInterfaceClass, BeanManager beanManager) {
        super();
        final RegisterDecisionService annotation = (this.decisionServiceInterfaceClass = aiServiceInterfaceClass)
                .getAnnotation(RegisterDecisionService.class);
        this.scope = annotation.scope();
        this.beanManager = beanManager;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.enterprise.inject.spi.PassivationCapable#getId()
     */
    @Override
    public String getId() {
        return decisionServiceInterfaceClass.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.enterprise.context.spi.Contextual#create(jakarta.enterprise.context.spi.CreationalContext)
     */
    @Override
    public T create(CreationalContext<T> creationalContext) {
        T instance = CommonDecisionServiceCreator.create(CDI.current(), decisionServiceInterfaceClass);
        if (!getInterceptorBindings().isEmpty()) {
            InterceptionFactory<T> factory =
                    beanManager.createInterceptionFactory(creationalContext, decisionServiceInterfaceClass);
            interceptorBindings.stream().forEach(factory.configure()::add);
            instance = factory.createInterceptedInstance(instance);
        }

        return instance;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.enterprise.context.spi.Contextual#destroy(java.lang.Object,
     * jakarta.enterprise.context.spi.CreationalContext)
     */
    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {}

    /*
     * (non-Javadoc)
     *
     * @see jakarta.enterprise.inject.spi.BeanAttributes#getTypes()
     */
    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(decisionServiceInterfaceClass);
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.enterprise.inject.spi.BeanAttributes#getQualifiers()
     */
    @Override
    public Set<Annotation> getQualifiers() {
        Set<Annotation> annotations = new HashSet<>();
        annotations.add(new AnnotationLiteral<Default>() {});
        annotations.add(new AnnotationLiteral<Any>() {});
        return Collections.unmodifiableSet(annotations);
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.enterprise.inject.spi.BeanAttributes#getScope()
     */
    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.enterprise.inject.spi.BeanAttributes#getName()
     */
    @Override
    public String getName() {
        return "registeredDecisionService-" + decisionServiceInterfaceClass.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.enterprise.inject.spi.BeanAttributes#getStereotypes()
     */
    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.singleton(RegisterDecisionService.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.enterprise.inject.spi.BeanAttributes#isAlternative()
     */
    @Override
    public boolean isAlternative() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.enterprise.inject.spi.Bean#getBeanClass()
     */
    @Override
    public Class<?> getBeanClass() {
        return decisionServiceInterfaceClass;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.enterprise.inject.spi.Bean#getInjectionPoints()
     */
    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    /** @return the interceptorBindings */
    @Override
    public Set<Annotation> getInterceptorBindings() {
        if (interceptorBindings == null) interceptorBindings = new HashSet<>();
        return interceptorBindings;
    }

    @Override
    public String toString() {
        return "DecisionService [ interfaceType: " + decisionServiceInterfaceClass.getSimpleName()
                + " ] with Qualifiers [" + getQualifiers() + "]";
    }
}
