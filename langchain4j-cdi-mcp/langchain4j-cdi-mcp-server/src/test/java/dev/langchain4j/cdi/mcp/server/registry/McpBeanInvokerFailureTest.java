package dev.langchain4j.cdi.mcp.server.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.langchain4j.cdi.mcp.server.api.McpApiFactory;
import dev.langchain4j.cdi.mcp.server.error.McpErrorCode;
import dev.langchain4j.cdi.mcp.server.error.McpException;
import dev.langchain4j.cdi.mcp.server.fixtures.FailingTool;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.json.JsonValue;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * langchain4j-cdi#291: an invocation failure is answered as {@code -32603} with a message that names neither the
 * failure nor the stack trace. The client-visible message stays that generic, on purpose; the original exception is
 * chained to the {@link McpException} so that it is not lost on the way up, on both invocation paths.
 */
class McpBeanInvokerFailureTest {

    McpBeanInvoker invoker;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setup() {
        BeanManager beanManager = mock(BeanManager.class);
        Bean<Object> bean = (Bean<Object>) mock(Bean.class);
        when(beanManager.getBeans(any(Type.class))).thenReturn((Set) Set.of(bean));
        when(beanManager.resolve(anySet())).thenReturn((Bean) bean);
        when(beanManager.createCreationalContext(any())).thenReturn((CreationalContext) mock(CreationalContext.class));
        when(beanManager.getReference(any(), any(), any())).thenReturn(new FailingTool());

        invoker = new McpBeanInvoker();
        invoker.beanManager = beanManager;
        invoker.apiFactory = mock(McpApiFactory.class);
        invoker.invokerProviders = noProvider();
    }

    @Test
    void reflectionPathChainsTheFailureToTheAnswer() throws Exception {
        McpException error = invokeFailingTool();

        assertThat(error.getErrorCode()).isEqualTo(McpErrorCode.INTERNAL_ERROR);
        assertThat(error.getMessage()).isEqualTo("Tool execution failed").doesNotContain(FailingTool.FAILURE_MESSAGE);
        assertThat(error.getRequestId()).isEqualTo(7);
        assertThat(error.getCause()).isInstanceOf(IllegalStateException.class).hasMessage(FailingTool.FAILURE_MESSAGE);
    }

    @Test
    void providerPathChainsTheFailureToTheAnswer() throws Exception {
        invoker.invokerProviders = providerOf((beanInstance, args) -> {
            throw new IllegalStateException(FailingTool.FAILURE_MESSAGE);
        });

        McpException error = invokeFailingTool();

        assertThat(error.getErrorCode()).isEqualTo(McpErrorCode.INTERNAL_ERROR);
        assertThat(error.getCause()).isInstanceOf(IllegalStateException.class).hasMessage(FailingTool.FAILURE_MESSAGE);
    }

    private McpException invokeFailingTool() throws Exception {
        Method fail = FailingTool.class.getMethod("fail");
        return catchThrowableOfType(
                () -> invoker.invoke(7, FailingTool.class, fail, JsonValue.EMPTY_JSON_OBJECT), McpException.class);
    }

    private static Instance<McpInvokerProvider> noProvider() {
        Instance<McpInvokerProvider> instance = mock(Instance.class);
        when(instance.isUnsatisfied()).thenReturn(true);
        return instance;
    }

    @SuppressWarnings("unchecked")
    private static Instance<McpInvokerProvider> providerOf(McpMethodInvoker methodInvoker) {
        McpInvokerProvider provider = (beanType, methodName, parameterTypes) -> Optional.of(methodInvoker);
        Instance<McpInvokerProvider> instance = mock(Instance.class);
        when(instance.isUnsatisfied()).thenReturn(false);
        when(instance.iterator()).thenAnswer(invocation -> List.of(provider).iterator());
        return instance;
    }
}
