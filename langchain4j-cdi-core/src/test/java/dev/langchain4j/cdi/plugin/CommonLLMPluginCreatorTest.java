package dev.langchain4j.cdi.plugin;

import dev.langchain4j.cdi.core.config.TextBlockLLMConfig;
import dev.langchain4j.cdi.core.config.spi.LLMConfigProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommonLLMPluginCreatorTest {

    @SuppressWarnings("unchecked")
    public static final Instance<Object> LOOKUP_MOCKED = mock(Instance.class);
    @SuppressWarnings("unchecked")
    public static final Instance<DummyInjected> DUMMY_INJECTED_INSTANCE_MOCKED = mock(Instance.class);
    private final static TextBlockLLMConfig llmConfig = (TextBlockLLMConfig) LLMConfigProvider.getLlmConfig();
    public static final List<String> BEAN_NAMES_LIST = List.of("beanA", "beanB", "beanC");

    @BeforeEach
    void beforeEach() {
        DummyInjected dummyInjectedMocked = mock(DummyInjected.class);
        //
        when(LOOKUP_MOCKED.select(DummyInjected.class)).thenReturn(DUMMY_INJECTED_INSTANCE_MOCKED);
        when(LOOKUP_MOCKED.select(DummyInjected.class, NamedLiteral.of(DummyInjected.class.getName())))
                .thenReturn(DUMMY_INJECTED_INSTANCE_MOCKED);
        //
        when(DUMMY_INJECTED_INSTANCE_MOCKED.get()).thenReturn(dummyInjectedMocked);
        // Mock BeanManager resolution for ParameterizedType DummyParam<Integer>
        BeanManager bm = mock(BeanManager.class);
        @SuppressWarnings("unchecked")
        Bean<Object> dummyParamBean = (Bean<Object>) mock(Bean.class);
        jakarta.enterprise.context.spi.CreationalContext<Object> ctx = mock(
                jakarta.enterprise.context.spi.CreationalContext.class);
        java.util.Set<Bean<?>> set = java.util.Set.of(dummyParamBean);
        when(bm.getBeans(org.mockito.ArgumentMatchers.any(java.lang.reflect.Type.class))).thenReturn((java.util.Set) set);
        when(bm.resolve(org.mockito.ArgumentMatchers.anySet())).thenReturn((Bean) dummyParamBean);
        when(bm.createCreationalContext(dummyParamBean)).thenReturn(ctx);
        when(bm.getReference(org.mockito.ArgumentMatchers.eq(dummyParamBean),
                org.mockito.ArgumentMatchers.any(java.lang.reflect.Type.class), org.mockito.ArgumentMatchers.eq(ctx)))
                .thenReturn(new DummyIntegerParam());
        dev.langchain4j.cdi.core.config.spi.LLMConfig.setBeanManagerSupplier(() -> bm);
        //
        llmConfig.reinitForTest("""
                dev.langchain4j.plugin.beanA.class=dev.langchain4j.cdi.plugin.DummyModel
                dev.langchain4j.plugin.beanA.scope=jakarta.enterprise.context.RequestScoped
                dev.langchain4j.plugin.beanA.config.api-key=01
                dev.langchain4j.plugin.beanA.config.param1=test
                dev.langchain4j.plugin.beanA.config.timeout=30
                dev.langchain4j.plugin.beanA.config.dummyEnum=A
                dev.langchain4j.plugin.beanA.config.dummyEnumList=C,D
                dev.langchain4j.plugin.beanA.config.dummyInjected=lookup:@default
                dev.langchain4j.plugin.beanA.config.dummy-param-int=lookup:@default
                dev.langchain4j.plugin.beanA.config.dummyWithStringConstructor=ok

                # No scope defined to get ApplicationScoped by default
                dev.langchain4j.plugin.beanB.class=dev.langchain4j.cdi.plugin.DummyModel
                dev.langchain4j.plugin.beanB.config.api-key=01
                dev.langchain4j.plugin.beanB.config.param1=test
                dev.langchain4j.plugin.beanB.config.timeout=30
                dev.langchain4j.plugin.beanB.config.dummyEnum=A
                dev.langchain4j.plugin.beanB.config.dummyEnumList=C,D
                dev.langchain4j.plugin.beanB.config.dummyInjected=lookup:dev.langchain4j.cdi.plugin.DummyInjected
                dev.langchain4j.plugin.beanB.config.dummy-param-int=lookup:@default
                dev.langchain4j.plugin.beanB.config.dummyWithStringConstructor=ok

                # No scope defined to get ApplicationScoped by default
                dev.langchain4j.plugin.beanC.class=dev.langchain4j.cdi.plugin.DummyModel
                dev.langchain4j.plugin.beanC.defined_bean_producer=ProducerC

                """);
        llmConfig.registerProducer(
                "ProducerC",
                (lookup, beanName, llmCOnfig) -> new DummyModel(
                        "01",
                        30,
                        dummyInjectedMocked,
                        new DummyIntegerParam(),
                        "test",
                        DummyEnum.A,
                        List.of(DummyEnum.C, DummyEnum.D),
                        new DummyWithStringConstructor("ok")));
    }

    private static boolean assertBean(DummyModel dummyModel, String beanName) {
        assertNotNull(dummyModel, "For " + beanName);
        assertNotNull(dummyModel.param1, "For " + beanName + " param1");
        assertNotNull(dummyModel.timeout, "For " + beanName + " timeout");
        assertNotNull(dummyModel.dummyInjected, "For " + beanName + " dummyInjected");
        assertNotNull(dummyModel.dummyEnum, "For " + beanName + " dummyEnum");
        assertNotNull(dummyModel.dummyParamInt, "For " + beanName + " dummyParamInt");
        assertNotNull(dummyModel.dummyEnumList, "For " + beanName + " dummyEnumList");
        assertNotNull(dummyModel.dummyWithStringConstructor, "For " + beanName + " dummyWithStringConstructor");

        assertEquals(DummyEnum.A, dummyModel.dummyEnum, "For " + beanName + " dummyEnum");
        assertEquals("ok", dummyModel.dummyWithStringConstructor.getValue(), "For " + beanName + " dummyWithStringConstructor");
        assertEquals(List.of(DummyEnum.C, DummyEnum.D), dummyModel.dummyEnumList, "For " + beanName + " dummyEnumList");
        return true;
    }

    @Test
    void createAllBeans() throws ClassNotFoundException {
        List<CommonLLMPluginCreator.BeanData> beanDataList = new ArrayList<>();
        CommonLLMPluginCreator.prepareAllLLMBeans(
                llmConfig,
                beanDataList::add);

        assertEquals(BEAN_NAMES_LIST.size(), beanDataList.size(), "Found " + beanDataList.size() + " beans: "
                + beanDataList.stream().map(CommonLLMPluginCreator.BeanData::beanName).toList());
        assertTrue(
                beanDataList.stream().map(CommonLLMPluginCreator.BeanData::beanName).toList()
                        .containsAll(BEAN_NAMES_LIST));
        AtomicReference<CommonLLMPluginCreator.BeanData> beanDataRef = new AtomicReference<>();
        assertTrue(
                beanDataList.stream()
                        .peek(beanDataRef::set)
                        .map(bd -> bd.callback().apply(LOOKUP_MOCKED))
                        .map(o -> (DummyModel) o)
                        .allMatch(dummyModel -> assertBean(dummyModel, beanDataRef.get().beanName())));

        assertTrue(
                beanDataList.stream().filter(bd -> bd.beanName().equals("beanA"))
                        .allMatch(bd -> bd.scopeClass().equals(RequestScoped.class)));
        assertTrue(
                beanDataList.stream().filter(bd -> !bd.beanName().equals("beanA"))
                        .allMatch(bd -> bd.scopeClass().equals(ApplicationScoped.class)));

    }

    @Test
    void prepareAllLLMBeans_noBuilderClass_leadsToNoBeanData() throws ClassNotFoundException {
        llmConfig.reinitForTest("""
                dev.langchain4j.plugin.noBuilder.class=dev.langchain4j.cdi.plugin.NoBuilderModel
                """);
        List<CommonLLMPluginCreator.BeanData> list = new ArrayList<>();
        CommonLLMPluginCreator.prepareAllLLMBeans(llmConfig, list::add);
        // The method logs a warning and returns (stops processing this bean). Ensure nothing added.
        assertTrue(list.isEmpty(), "No BeanData should be created when builder class is missing: "
                + list.stream().map(CommonLLMPluginCreator.BeanData::beanName).toList());
    }

    @Test
    void create_missingField_throwsRuntimeWrappingNoSuchField() throws ClassNotFoundException {
        llmConfig.reinitForTest("""
                dev.langchain4j.plugin.bad.class=dev.langchain4j.cdi.plugin.DummyModel
                dev.langchain4j.plugin.bad.config.unknown-prop=value
                """);
        Class<?> target = CommonLLMPluginCreator.loadClass("dev.langchain4j.cdi.plugin.DummyModel");
        Class<?> builder = target.getDeclaredClasses()[0];
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> CommonLLMPluginCreator.create(LOOKUP_MOCKED, llmConfig, "bad", target, builder));
        assertTrue(ex.getCause() instanceof NoSuchFieldException);
        assertTrue(ex.getMessage().contains("unknown-prop"));
    }

    @Test
    void getFieldsInAllHierarchy_handlesNullAndIncludesParent() throws Exception {
        // Null case via reflection
        var method = CommonLLMPluginCreator.class.getDeclaredMethod("getFieldsInAllHierarchy", Class.class);
        method.setAccessible(true);
        List<?> empty = (List<?>) method.invoke(null, new Object[] { null });
        assertNotNull(empty);
        assertTrue(empty.isEmpty());
        // Parent+child aggregation
        Class<?> builder = DummyModel.DummyModelBuilder.class;
        @SuppressWarnings("unchecked")
        List<java.lang.reflect.Field> fields = (List<java.lang.reflect.Field>) method.invoke(null, builder);
        // Should include fields from DummyBaseModel.Builder: apiKey, timeout, dummyInjected
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("apiKey")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("timeout")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("dummyInjected")));
        // And from DummyModel.DummyModelBuilder: param1, dummyEnum, dummyEnumList
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("param1")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("dummyEnum")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("dummyEnumList")));
    }

    @Test
    void loadClass_usesFallbackWhenContextClassLoaderCannotFind() throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new ClassLoader(null) {
                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    throw new ClassNotFoundException("forced");
                }
            });
            Class<?> cls = CommonLLMPluginCreator.loadClass("dev.langchain4j.cdi.plugin.DummyModel");
            assertEquals(DummyModel.class, cls);
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

}