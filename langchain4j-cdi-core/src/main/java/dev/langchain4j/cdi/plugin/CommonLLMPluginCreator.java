package dev.langchain4j.cdi.plugin;

import static dev.langchain4j.cdi.core.config.spi.LLMConfig.PRODUCER;

import dev.langchain4j.cdi.core.config.spi.LLMConfig;
import dev.langchain4j.cdi.core.config.spi.ProducerFunction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Helper to build LangChain4j beans (models, retrievers, stores, etc.) from an LLMConfig source.
 *
 * <p>Many LangChain4j components expose a static builder() method and an inner Builder class with fluent setters. This
 * utility reflects on that pattern to populate builders from configuration and produce ready-to-use instances.
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
            ProducerFunction<Object> producer = (ProducerFunction<Object>)
                    llmConfig.getBeanPropertyValue(beanName, PRODUCER, ProducerFunction.class);
            Class<?> builderCLass;
            if (producer == null) {
                builderCLass = Arrays.stream(targetClass.getDeclaredClasses())
                        .filter(declClass -> declClass.getName().endsWith("Builder"))
                        .findFirst()
                        .orElse(null);
                LOGGER.info("Builder class : " + builderCLass);
                if (builderCLass == null) {
                    LOGGER.warning("No builder class found, cancel " + beanName);
                    return;
                }
                producer = (creationalContext, beanName1, llmConfig1) ->
                        create(creationalContext, llmConfig, beanName, targetClass, builderCLass);
            } else {
                builderCLass = null;
            }

            ProducerFunction<Object> finalProducer = producer;
            beanBuilder.accept(new BeanData(
                    targetClass,
                    builderCLass,
                    scopeClass,
                    beanName,
                    (Instance<Object> creationalContext) ->
                            finalProducer.produce(creationalContext, beanName, llmConfig)));
        }
    }

    public record BeanData(
            Class<?> targetClass,
            Class<?> builderClass,
            Class<? extends Annotation> scopeClass,
            String beanName,
            Function<Instance<Object>, Object> callback) {}

    public static Object create(
            Instance<Object> lookup,
            LLMConfig llmConfig,
            String beanName,
            Class<?> targetClass,
            Class<?> builderClass) {
        LOGGER.info("Create instance for :" + beanName + ", target class : " + targetClass + ", builderClass : "
                + builderClass);
        String currentProperty = "";
        try {
            Object builder = null;
            try {
                builder = targetClass.getMethod("builder").invoke(null);
            } catch (NoSuchMethodException e) {
                builder = targetClass
                        .getClassLoader()
                        .loadClass(targetClass.getName() + "$Builder")
                        .getConstructor(new Class[0])
                        .newInstance(new Object[0]);
            }
            Set<String> properties = llmConfig.getPropertyNamesForBean(beanName);
            for (String property : properties) {
                currentProperty = property;
                String camelCaseProperty = dashToCamel(property);
                String propertyKey = "config." + property;
                boolean propertySet = false;

                // determine field in builder
                List<Field> fields = getFieldsInAllHierarchy(builderClass);
                Field propertyFieldInBuilder = fields.stream()
                        .filter(field -> field.getName().equals(camelCaseProperty))
                        .findFirst()
                        .orElse(null);
                if (propertyFieldInBuilder != null) {
                    Method setterMethod = builderClass.getMethod(camelCaseProperty, propertyFieldInBuilder.getType());

                    Type genericType = propertyFieldInBuilder.getGenericType();
                    Object value = llmConfig.getBeanPropertyValue(lookup, beanName, propertyKey, genericType);
                    try {
                        if (genericType instanceof ParameterizedType pt) {
                            genericType = pt.getRawType();
                        }
                        boolean isList = List.class.isAssignableFrom((Class) genericType);
                        boolean isSet = Set.class.isAssignableFrom((Class) genericType);
                        if ((value instanceof List && isList) || (value instanceof Set && isSet)) {
                            setterMethod.invoke(builder, value);
                            propertySet = true;
                        } else {
                            if (isList) {
                                setterMethod.invoke(builder, List.of(value));
                                propertySet = true;
                            } else if (isSet) {
                                setterMethod.invoke(builder, Set.of(value));
                                propertySet = true;
                            } else {
                                setterMethod.invoke(builder, value);
                                propertySet = true;
                            }
                        }
                    } catch (ReflectiveOperationException e) {
                    }
                } else {
                    // Let's try using methods in the builder
                    List<Method> methods = findMethodsInAllHierarch(builderClass, camelCaseProperty);
                    if (methods != null && !methods.isEmpty()) {
                        for (Method setterMethod : methods) {
                            if (setterMethod.getParameterCount() != 1) continue;
                            Object value = llmConfig.getBeanPropertyValue(
                                    lookup, beanName, propertyKey, setterMethod.getParameterTypes()[0]);
                            try {
                                setterMethod.invoke(builder, value);
                                propertySet = true;
                            } catch (ReflectiveOperationException e) {
                            }
                        }
                    }
                }

                if (!propertySet)
                    throw new ReflectiveOperationException("Can't find field or method for config property '" + property
                            + "' +' (" + camelCaseProperty + ") in builder (" + builderClass.getName() + ")");
            }
            return builderClass.getMethod("build").invoke(builder);
        } catch (IllegalArgumentException | SecurityException | ReflectiveOperationException e) {
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
        if (startClass == null) return List.of();

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

    private static List<Method> findMethodsInAllHierarch(Class<?> startClass, final String methodName) {
        if (startClass == null || methodName == null || methodName.isEmpty()) return List.of();

        List<Method> methods = new ArrayList<>();
        Arrays.stream(startClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .forEach(methods::add);

        Class<?> parentClass = startClass.getSuperclass();
        if (parentClass != null && !parentClass.equals(Object.class)) {
            Arrays.stream(parentClass.getDeclaredMethods())
                    .filter(method -> method.getName().equals(methodName))
                    .forEach(methods::add);
        }

        return methods;
    }
}
