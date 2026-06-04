package dev.langchain4j.cdi.core;

import dev.langchain4j.agentic.internal.InternalAgent;
import dev.langchain4j.cdi.agent.CommonAgentCreator;
import dev.langchain4j.cdi.core.portableextension.LangChain4JAIServicePortableExtension;
import dev.langchain4j.cdi.core.portableextension.LangChain4JPluginsPortableExtension;
import io.smallrye.config.inject.ConfigExtension;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
public class AgentPortableExtensionTest {

    @Inject
    MyDummyAgent myDummyAgent;

    @Inject
    MyDummyNamedAgent myDummyNamedAgent;

    @Inject
    @Named("namedAgent")
    MyDummyNamedAgent namedAgentByQualifier;

    @Inject
    BeanManager beanManager;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
                    LangChain4JAIServicePortableExtension.class,
                    LangChain4JPluginsPortableExtension.class,
                    MyDummyAgent.class,
                    MyDummyNamedAgent.class,
                    DummyChatModel.class,
                    DummyEmbeddingStore.class,
                    DummyEmbeddingModel.class,
                    ConfigExtension.class)
            .build();

    @Test
    void detectAgentInterface() {
        Assertions.assertTrue(LangChain4JAIServicePortableExtension.getDetectedAgentDeclaredInterfaces()
                .contains(MyDummyAgent.class));
        Assertions.assertTrue(LangChain4JAIServicePortableExtension.getDetectedAgentDeclaredInterfaces()
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
    void ensureNamedQualifier() {
        Assertions.assertNotNull(namedAgentByQualifier);

        Bean<?> bean = beanManager.getBeans(MyDummyNamedAgent.class).iterator().next();
        Assertions.assertEquals("namedAgent", bean.getName());
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

    @Test
    void agentBeanNameFollowsConvention() {
        Bean<?> unnamedBean =
                beanManager.getBeans(MyDummyAgent.class).iterator().next();
        Assertions.assertEquals(
                CommonAgentCreator.AGENT_BEAN_NAME_PREFIX + MyDummyAgent.class.getName(), unnamedBean.getName());

        Bean<?> namedBean =
                beanManager.getBeans(MyDummyNamedAgent.class).iterator().next();
        Assertions.assertEquals("namedAgent", namedBean.getName());
    }

    @Test
    void namedAgentBeanAlwaysHasNamedQualifier() {
        // Same Liberty-container concern applies to named agents: @Named must be in getQualifiers(),
        // not just returned by getName().
        Set<Bean<?>> byName = beanManager.getBeans("namedAgent");
        Assertions.assertFalse(byName.isEmpty(), "BeanManager.getBeans(name) must find named agent bean");

        Bean<?> bean = byName.iterator().next();
        boolean hasNamedQualifier = bean.getQualifiers().stream()
                .anyMatch(q -> q instanceof Named named && named.value().equals("namedAgent"));
        Assertions.assertTrue(hasNamedQualifier, "Named agent bean must have @Named(\"namedAgent\") qualifier");
    }

    @Test
    void unnamedAgentBeanAlwaysHasNamedQualifier() {
        // BeanManager.getBeans(String) on some CDI containers (e.g. Liberty) only indexes beans
        // that carry an actual @Named qualifier — not beans that merely override getName(). Ensure
        // LangChain4JAIAgentBean always adds @Named(getName()) so the BeanManager fallback in
        // toAgentExecutor works on all containers.
        String expectedName = CommonAgentCreator.AGENT_BEAN_NAME_PREFIX + MyDummyAgent.class.getName();
        Set<Bean<?>> byName = beanManager.getBeans(expectedName);
        Assertions.assertFalse(byName.isEmpty(), "BeanManager.getBeans(name) must find unnamed agent bean");

        Bean<?> bean = byName.iterator().next();
        boolean hasNamedQualifier = bean.getQualifiers().stream()
                .anyMatch(q -> q instanceof Named named && named.value().equals(expectedName));
        Assertions.assertTrue(hasNamedQualifier, "Unnamed agent bean must have @Named(getName()) qualifier");
    }

    private void assertBeanScope(Class<?> beanType, Class<?> scopedClass) {
        Class<? extends Annotation> scope =
                beanManager.getBeans(beanType).iterator().next().getScope();
        Assertions.assertTrue(scope.isAssignableFrom(scopedClass));
    }
}
