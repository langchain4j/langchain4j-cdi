package dev.langchain4j.cdi.plugin;

import dev.langchain4j.cdi.core.config.spi.LLMConfig;
import dev.langchain4j.cdi.core.config.spi.ProducerFunction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static dev.langchain4j.cdi.core.config.spi.LLMConfig.PRODUCER;

/**
 * Helper to build LangChain4j beans (models, retrievers, stores, etc.) from an LLMConfig source.
 * <p>
 * Many LangChain4j components expose a static builder() method and an inner Builder class with fluent setters.
 * This utility reflects on that pattern to populate builders from configuration and produce ready-to-use instances.
 */
public class CommonLLMPluginCreator {

    public static final Logger LOGGER = Logger.getLogger(CommonLLMPluginCreator.class.getName());

    @SuppressWarnings("unchecked")
    public static void prepareAllLLMBeans(LLMConfig llmConfig, Consumer<BeanData> beanBuilder)
            throws ClassNotFoundException {
        Set<String> beanNameToCreate = llmConfig.getBeanNames();
        LOGGER.info("detected beans to create : " + beanNameToCreate);

        for (String beanName : beanNameToCreate) {
            String className = llmConfig.getBeanPropertyValue(beanName, LLMConfig.CLASS);
            String scopeClassName = llmConfig.getBeanPropertyValue(beanName, LLMConfig.SCOPE);
            if (scopeClassName == null || scopeClassName.isBlank()) {
                scopeClassName = ApplicationScoped.class.getName();
            }
            Class<? extends Annotation> scopeClass = (Class<? extends Annotation>) loadClass(scopeClassName);
            Class<?> targetClass = loadClass(className);
            ProducerFunction<Object> producer = (ProducerFunction<Object>) llmConfig.getBeanPropertyValue(beanName, PRODUCER,
                    ProducerFunction.class);
            Class<?> builderCLass;
            if (producer == null) {
                builderCLass = Arrays.stream(targetClass.getDeclaredClasses())
                        .filter(declClass -> declClass.getName().endsWith("Builder")).findFirst().orElse(null);
                LOGGER.info("Builder class : " + builderCLass);
                if (builderCLass == null) {
                    LOGGER.warning("No builder class found, cancel " + beanName);
                    return;
                }
                producer = (creationalContext, beanName1, llmConfig1) -> create(
                        creationalContext, llmConfig, beanName, targetClass, builderCLass);
            } else {
                builderCLass = null;
            }

            ProducerFunction<Object> finalProducer = producer;
            beanBuilder.accept(
                    new BeanData(targetClass, builderCLass, scopeClass, beanName,
                            (Instance<Object> creationalContext) -> finalProducer.produce(creationalContext, beanName,
                                    llmConfig)));
        }
    }

    public record BeanData(Class<?> targetClass,
            Class<?> builderClass,
            Class<? extends Annotation> scopeClass,
            String beanName,
            Function<Instance<Object>, Object> callback) {

    }

    public static Object create(Instance<Object> lookup, LLMConfig llmConfig, String beanName, Class<?> targetClass,
            Class<?> builderClass) {
        LOGGER.info(
                "Create instance for :" + beanName + ", target class : " + targetClass + ", builderClass : " + builderClass);
        String currentProperty = "";
        try {
            Object builder = targetClass.getMethod("builder").invoke(null);
            Set<String> properties = llmConfig.getPropertyNamesForBean(beanName);
            for (String property : properties) {
                currentProperty = property;
                String camelCaseProperty = dashToCamel(property);
                // determine field in builder
                List<Field> fields = getFieldsInAllHierarchy(builderClass);
                Field propertyFieldInBuilder = fields.stream()
                        .filter(field -> field.getName().equals(camelCaseProperty))
                        .findFirst()
                        .orElse(null);
                if (propertyFieldInBuilder == null)
                    throw new NoSuchFieldException(
                            "Can't find Field for property '" + property + "' (" + camelCaseProperty + ") in bean " + beanName
                                    + "(" + targetClass.getName() + ")");
                //
                Method setterMethod = builderClass.getMethod(camelCaseProperty, propertyFieldInBuilder.getType());
                String propertyKey = "config." + property;

                Type genericType = propertyFieldInBuilder.getGenericType();
                Object value = llmConfig.getBeanPropertyValue(lookup, beanName, propertyKey, genericType);
                setterMethod.invoke(builder, value);
            }
            return builderClass.getMethod("build").invoke(builder);
        } catch (
                IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException
                | NoSuchFieldException | InvocationTargetException e) {
            throw new RuntimeException("Current property : " + currentProperty, e);
        }
    }

    static String dashToCamel(String property) {
        String fixed;
        fixed = Arrays.stream(property.split("-"))
                .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1))
                .collect(Collectors.joining());
        fixed = fixed.substring(0, 1).toLowerCase() + fixed.substring(1);
        return fixed;
    }

    private static List<Field> getFieldsInAllHierarchy(Class<?> startClass) {
        if (startClass == null)
            return new ArrayList<>();

        List<Field> currentClassFields = new ArrayList<>(Arrays.asList(startClass.getDeclaredFields()));
        Class<?> parentClass = startClass.getSuperclass();

        if (parentClass != null && !parentClass.equals(Object.class)) {
            List<Field> parentClassFields = getFieldsInAllHierarchy(parentClass);
            currentClassFields.addAll(parentClassFields);
        }

        return currentClassFields;
    }

    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException classNotFoundException) {
            return CommonLLMPluginCreator.class.getClassLoader().loadClass(className);
        }
    }

}
