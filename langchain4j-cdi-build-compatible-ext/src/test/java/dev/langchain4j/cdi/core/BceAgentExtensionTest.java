package dev.langchain4j.cdi.core;

import dev.langchain4j.agentic.internal.InternalAgent;
import dev.langchain4j.cdi.agent.CommonAgentCreator;
import dev.langchain4j.cdi.core.buildcompatibleextension.Langchain4JAIServiceBuildCompatibleExtension;
import io.smallrye.config.inject.ConfigExtension;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WeldJunit5Extension.class)
public class BceAgentExtensionTest {

    @Inject
    MyDummyAgent myDummyAgent;

    @Inject
    MyDummyNamedAgent myDummyNamedAgent;

    @Inject
    BeanManager beanManager;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
                    MyDummyAgent.class,
                    MyDummyNamedAgent.class,
                    DummyChatModel.class,
                    DummyEmbeddingStore.class,
                    DummyEmbeddingModel.class,
                    ConfigExtension.class)
            .build();

    @Test
    void detectAgentInterface() {
        Assertions.assertTrue(Langchain4JAIServiceBuildCompatibleExtension.getDetectedAgentDeclaredInterfaces()
                .contains(MyDummyAgent.class));
        Assertions.assertTrue(Langchain4JAIServiceBuildCompatibleExtension.getDetectedAgentDeclaredInterfaces()
                .contains(MyDummyNamedAgent.class));
    }

    @Test
    void ensureInjectAndScope() {
        Assertions.assertNotNull(myDummyAgent);
        Assertions.assertNotNull(myDummyNamedAgent);
        assertBeanScope(MyDummyAgent.class, ApplicationScoped.class);
        assertBeanScope(MyDummyNamedAgent.class, ApplicationScoped.class);
    }

    @Test
    void ensureBeanNameFollowsConvention() {
        Bean<?> unnamedBean =
                beanManager.getBeans(MyDummyAgent.class).iterator().next();
        Assertions.assertEquals(
                CommonAgentCreator.AGENT_BEAN_NAME_PREFIX + MyDummyAgent.class.getName(), unnamedBean.getName());

        Bean<?> namedBean =
                beanManager.getBeans(MyDummyNamedAgent.class).iterator().next();
        Assertions.assertEquals("namedAgent", namedBean.getName());
    }

    @Test
    void ensureBeanTypesIncludeInternalAgent() {
        Bean<?> bean = beanManager.getBeans(MyDummyAgent.class).iterator().next();
        Set<Type> types = bean.getTypes();
        Assertions.assertTrue(types.contains(MyDummyAgent.class));
        Assertions.assertTrue(types.contains(InternalAgent.class));
        Assertions.assertTrue(types.contains(Object.class));
    }

    @Test
    void callEffectiveCreation() {
        String str = myDummyAgent.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("Agent["));
    }

    private void assertBeanScope(Class<?> beanType, Class<?> scopedClass) {
        Class<? extends Annotation> scope =
                beanManager.getBeans(beanType).iterator().next().getScope();
        Assertions.assertTrue(scope.isAssignableFrom(scopedClass));
    }
}
