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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper to build LangChain4j beans (models, retrievers, stores, etc.) from an LLMConfig source.
 *
 * <p>Many LangChain4j components expose a static builder() method and an inner Builder class with fluent setters. This
 * utility reflects on that pattern to populate builders from configuration and produce ready-to-use instances.
 */
public class CommonLLMPluginCreator {

    /** Utility class — not instantiable. */
    private CommonLLMPluginCreator() {}

    /** Logger for this class. */
    public static final Logger LOGGER = Logger.getLogger(CommonLLMPluginCreator.class.getName());

    /**
     * Scan the given configuration for LLM bean definitions and pass each to the consumer.
     *
     * @param llmConfig the configuration source describing beans to create
     * @param beanBuilder consumer that registers each discovered bean
     * @throws ClassNotFoundException if a configured class cannot be loaded
     */
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

            // Validate scope class is actually an annotation
            Class<?> loadedScopeClass = loadClass(scopeClassName);
            if (!Annotation.class.isAssignableFrom(loadedScopeClass)) {
                throw new IllegalArgumentException(
                        "Scope class " + scopeClassName + " for bean " + beanName + " is not an annotation type");
            }
            Class<? extends Annotation> scopeClass = (Class<? extends Annotation>) loadedScopeClass;

            Class<?> targetClass = loadClass(className);
            ProducerFunction<Object> producer = (ProducerFunction<Object>)
                    llmConfig.getBeanPropertyValue(beanName, PRODUCER, ProducerFunction.class);
            Class<?> builderClass;
            if (producer == null) {
                builderClass = Arrays.stream(targetClass.getDeclaredClasses())
                        .filter(declClass -> declClass.getName().endsWith("Builder"))
                        .findFirst()
                        .orElse(null);
                LOGGER.info("Builder class : " + builderClass);
                if (builderClass == null) {
                    LOGGER.warning("No builder class found, skipping bean: " + beanName);
                    continue;
                }
                producer = (creationalContext, beanName1, llmConfig1) ->
                        create(creationalContext, llmConfig, beanName, targetClass, builderClass);
            } else {
                builderClass = null;
            }

            ProducerFunction<Object> finalProducer = producer;
            beanBuilder.accept(new BeanData(
                    targetClass,
                    builderClass,
                    scopeClass,
                    beanName,
                    (Instance<Object> creationalContext) ->
                            finalProducer.produce(creationalContext, beanName, llmConfig)));
        }
    }

    /**
     * Holds the metadata needed to register a single LLM bean.
     *
     * @param targetClass the bean class to instantiate
     * @param builderClass the inner Builder class used to construct the target, or {@code null} when a custom producer
     *     is supplied
     * @param scopeClass the CDI scope annotation for the bean
     * @param beanName the logical bean name from configuration
     * @param callback function that creates the bean instance given a CDI lookup context
     */
    public record BeanData(
            Class<?> targetClass,
            Class<?> builderClass,
            Class<? extends Annotation> scopeClass,
            String beanName,
            Function<Instance<Object>, Object> callback) {}

    /**
     * Create a single LLM bean instance via its builder, populated from configuration.
     *
     * @param lookup CDI Instance for resolving dependent beans
     * @param llmConfig configuration source
     * @param beanName logical name of the bean
     * @param targetClass the class to instantiate
     * @param builderClass the inner Builder class used to construct the target
     * @return a fully configured instance of the target class
     */
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
            Object builder;
            try {
                builder = targetClass.getMethod("builder").invoke(null);
            } catch (NoSuchMethodException e) {
                builder = targetClass
                        .getClassLoader()
                        .loadClass(targetClass.getName() + "$Builder")
                        .getConstructor(new Class[0])
                        .newInstance();
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
                    } catch (Exception e) {
                        throw new ReflectiveOperationException(
                                "Failed to set property '" + property + "' via field-based setter: "
                                        + setterMethod.getName() + "("
                                        + Stream.of(setterMethod.getParameterTypes())
                                                .map(Class::getName)
                                                .collect(Collectors.joining(","))
                                        + ") try to set "
                                        + value.getClass().getName(),
                                e);
                    }
                } else {
                    // Let's try using methods in the builder
                    List<Method> methods = findMethodsInAllHierarch(builderClass, camelCaseProperty);
                    if (!methods.isEmpty()) {
                        for (Method setterMethod : methods) {
                            if (setterMethod.getParameterCount() != 1) {
                                continue;
                            }
                            Object value = llmConfig.getBeanPropertyValue(
                                    lookup, beanName, propertyKey, setterMethod.getParameterTypes()[0]);
                            try {
                                setterMethod.invoke(builder, value);
                                propertySet = true;
                            } catch (ReflectiveOperationException e) {
                                LOGGER.fine("Failed to set property '" + property + "' via method "
                                        + setterMethod.getName() + ": " + e.getMessage());
                            }
                        }
                    }
                }

                if (!propertySet) {
                    throw new ReflectiveOperationException("Can't find field or method for config property '" + property
                            + "' +' (" + camelCaseProperty + ") in builder (" + builderClass.getName() + ")");
                }
            }
            return builderClass.getMethod("build").invoke(builder);
        } catch (IllegalArgumentException | SecurityException | ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Failed to create bean '" + beanName + "' of type " + targetClass.getName()
                            + " while processing property '" + currentProperty + "'",
                    e);
        }
    }
    /**
     * Converts dash-separated property names to camelCase.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>"api-key" -&gt; "apiKey"
     *   <li>"base-url" -&gt; "baseUrl"
     *   <li>"timeout" -&gt; "timeout" (no change)
     * </ul>
     *
     * @param property the dash-separated property name
     * @return the camelCase version
     * @throws IllegalArgumentException if property is null or empty
     */
    static String dashToCamel(String property) {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("Property name cannot be null or empty");
        }

        String fixed = Arrays.stream(property.split("-"))
                .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1))
                .collect(Collectors.joining());
        fixed = fixed.substring(0, 1).toLowerCase() + fixed.substring(1);
        return fixed;
    }

    static List<Field> getFieldsInAllHierarchy(Class<?> startClass) {
        if (startClass == null) {
            return Collections.emptyList();
        }

        List<Field> currentClassFields = new ArrayList<>(Arrays.asList(startClass.getDeclaredFields()));
        Class<?> parentClass = startClass.getSuperclass();

        if (parentClass != null && !parentClass.equals(Object.class)) {
            List<Field> parentClassFields = getFieldsInAllHierarchy(parentClass);
            currentClassFields.addAll(parentClassFields);
        }

        return currentClassFields;
    }

    /**
     * Load a class by name, trying the thread context classloader first.
     *
     * @param className fully qualified class name
     * @return the loaded class
     * @throws ClassNotFoundException if the class cannot be found
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException classNotFoundException) {
            return CommonLLMPluginCreator.class.getClassLoader().loadClass(className);
        }
    }

    static List<Method> findMethodsInAllHierarch(Class<?> startClass, final String methodName) {
        if (startClass == null || methodName == null || methodName.isEmpty()) {
            return Collections.emptyList();
        }

        List<Method> methods = new ArrayList<>();
        Arrays.stream(startClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .forEach(methods::add);

        Class<?> parentClass = startClass.getSuperclass();
        if (parentClass != null && !parentClass.equals(Object.class)) {
            List<Method> parentMethods = findMethodsInAllHierarch(parentClass, methodName);
            methods.addAll(parentMethods);
        }

        return methods;
    }
}
