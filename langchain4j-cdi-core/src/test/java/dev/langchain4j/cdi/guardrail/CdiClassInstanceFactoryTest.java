package dev.langchain4j.cdi.guardrail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.GuardrailRequestParams;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.service.guardrail.GuardrailService;
import dev.langchain4j.service.guardrail.InputGuardrails;
import dev.langchain4j.service.guardrail.spi.GuardrailServiceBuilderFactory;
import dev.langchain4j.spi.classloading.ClassInstanceFactory;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class CdiClassInstanceFactoryTest {

    private final CdiClassInstanceFactory factory = new CdiClassInstanceFactory();

    public static class CountingGuardrail implements InputGuardrail {
        final AtomicInteger validations = new AtomicInteger();

        @Override
        public InputGuardrailResult validate(UserMessage userMessage) {
            validations.incrementAndGet();
            return success();
        }
    }

    public static class WithoutNoArgConstructor {
        public WithoutNoArgConstructor(String config) {}
    }

    @InputGuardrails(CountingGuardrail.class)
    interface GuardedAiService {
        String chat(String message);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> void givenCdiBean(MockedStatic<CDI> cdiMock, Class<T> beanClass, T bean, boolean resolvable) {
        CDI cdi = mock(CDI.class);
        Instance<T> instance = mock(Instance.class);

        cdiMock.when(CDI::current).thenReturn(cdi);
        doReturn(instance).when(cdi).select(beanClass);
        when(instance.isResolvable()).thenReturn(resolvable);
        when(instance.get()).thenReturn(bean);
    }

    @Test
    void getInstanceOfClass_resolvesTheBeanFromCdi() {
        var expected = new CountingGuardrail();

        try (MockedStatic<CDI> cdiMock = mockStatic(CDI.class)) {
            givenCdiBean(cdiMock, CountingGuardrail.class, expected, true);

            assertSame(expected, factory.getInstanceOfClass(CountingGuardrail.class));
        }
    }

    @Test
    void getInstanceOfClass_fallsBackToTheNoArgConstructorWhenTheClassIsNotAResolvableBean() {
        try (MockedStatic<CDI> cdiMock = mockStatic(CDI.class)) {
            givenCdiBean(cdiMock, CountingGuardrail.class, null, false);

            var result = factory.getInstanceOfClass(CountingGuardrail.class);

            assertNotNull(result);
            assertInstanceOf(CountingGuardrail.class, result);
        }
    }

    @Test
    void getInstanceOfClass_fallsBackToTheNoArgConstructorWhenCdiIsNotAvailable() {
        try (MockedStatic<CDI> cdiMock = mockStatic(CDI.class)) {
            cdiMock.when(CDI::current).thenThrow(new IllegalStateException("No CDI container"));

            var result = factory.getInstanceOfClass(CountingGuardrail.class);

            assertNotNull(result);
            assertInstanceOf(CountingGuardrail.class, result);
        }
    }

    @Test
    void getInstanceOfClass_failsWhenThereIsNeitherABeanNorANoArgConstructor() {
        try (MockedStatic<CDI> cdiMock = mockStatic(CDI.class)) {
            givenCdiBean(cdiMock, WithoutNoArgConstructor.class, null, false);

            var exception = assertThrows(
                    IllegalStateException.class, () -> factory.getInstanceOfClass(WithoutNoArgConstructor.class));

            assertTrue(exception.getMessage().contains(WithoutNoArgConstructor.class.getName()));
        }
    }

    @Test
    void getInstanceOfClass_rejectsNull() {
        assertThrows(NullPointerException.class, () -> factory.getInstanceOfClass(null));
    }

    @Test
    void isRegisteredAsTheClassInstanceFactory() {
        assertTrue(ServiceLoader.load(ClassInstanceFactory.class).stream()
                .anyMatch(provider -> provider.type().equals(CdiClassInstanceFactory.class)));
    }

    @Test
    void noGuardrailServiceBuilderFactoryIsRegistered() {
        assertFalse(ServiceLoader.load(GuardrailServiceBuilderFactory.class).stream()
                .anyMatch(provider -> provider.type().getName().startsWith("dev.langchain4j.cdi")));
    }

    @Test
    void theDefaultGuardrailServiceBuilderUsesTheCdiBeansOfTheGuardrailClasses() throws Exception {
        var bean = new CountingGuardrail();
        Method chat = GuardedAiService.class.getMethod("chat", String.class);

        try (MockedStatic<CDI> cdiMock = mockStatic(CDI.class)) {
            givenCdiBean(cdiMock, CountingGuardrail.class, bean, true);

            var service = GuardrailService.builder(GuardedAiService.class).build();
            var request = InputGuardrailRequest.builder()
                    .userMessage(UserMessage.from("hi"))
                    .commonParams(GuardrailRequestParams.builder()
                            .userMessageTemplate("template")
                            .variables(Map.of())
                            .invocationContext(InvocationContext.builder().build())
                            .build())
                    .build();

            assertTrue(service.hasInputGuardrails(chat));
            assertEquals(0, bean.validations.get());

            service.executeInputGuardrails(chat, request);

            assertEquals(1, bean.validations.get());
        }
    }

    @Test
    void theDefaultGuardrailServiceBuilderCreatesTheGuardrailsItselfWithoutACdiContainer() throws Exception {
        Method chat = GuardedAiService.class.getMethod("chat", String.class);

        try (MockedStatic<CDI> cdiMock = mockStatic(CDI.class)) {
            cdiMock.when(CDI::current).thenThrow(new IllegalStateException("No CDI container"));

            var first = GuardrailService.builder(GuardedAiService.class).build();
            var second = GuardrailService.builder(GuardedAiService.class).build();

            assertTrue(first.hasInputGuardrails(chat));
            assertNotSame(first, second);
        }
    }
}
