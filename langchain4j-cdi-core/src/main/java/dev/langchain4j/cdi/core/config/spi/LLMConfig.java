package dev.langchain4j.cdi.core.config.spi;

import dev.langchain4j.service.IllegalConfigurationException;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base abstraction to provide configuration for LLM-related beans.
 *
 * <p>This API is intentionally lightweight and relies purely on CDI (no external config dependency). It is inspired by
 * MicroProfile/SmallRye Config, but kept minimal for portability.
 */
public abstract class LLMConfig {

    /** Creates a new configuration instance. */
    protected LLMConfig() {}

    /** Prefix used to identify CDI bean lookup values in configuration. */
    public static final String LOOKUP_PREFIX = "lookup:";

    Map<String, ProducerFunction<?>> producers = new ConcurrentHashMap<>();

    /** Prefix for all LLM beans properties. */
    public static final String PREFIX = "dev.langchain4j.cdi.plugin";

    /** Property key for a custom {@link ProducerFunction} bean producer. */
    public static final String PRODUCER = "defined_bean_producer";

    /** Property key for the fully qualified class name of the bean. */
    public static final String CLASS = "class";

    /** Property key for the CDI scope annotation of the bean. */
    public static final String SCOPE = "scope";

    /** Initialize this configuration source. */
    public abstract void init();

    /**
     * Return all known property keys.
     *
     * @return set of property keys
     */
    public abstract Set<String> getPropertyKeys();

    /**
     * Return the raw string value for the given key.
     *
     * @param key the property key
     * @return the value, or {@code null} if absent
     */
    public abstract String getValue(String key);

    /**
     * Get all Langchain4j-cdi LLM beans names, prefixed by PREFIX For example:
     * dev.langchain4j.cdi.plugin.content-retriever.class -> content-retriever
     *
     * @return a set of property names
     */
    public Set<String> getBeanNames() {
        return getPropertyKeys().stream()
                .filter(key -> key.startsWith(PREFIX))
                .map(key -> key.substring(PREFIX.length() + 1))
                .map(key -> key.substring(0, key.indexOf(".")))
                .collect(Collectors.toSet());
    }

    /**
     * Return the raw configuration value for a bean property.
     *
     * @param beanName the logical bean name
     * @param propertyName the property suffix (e.g. "class", "scope", or "config.xxx")
     * @return the string value, or {@code null} if absent
     */
    public String getBeanPropertyValue(String beanName, String propertyName) {
        String key = PREFIX + "." + beanName + "." + propertyName;
        return getValue(key);
    }

    /**
     * Register a custom bean producer function under the given name.
     *
     * @param producersName the logical name used to reference this producer in configuration
     * @param producer the producer function to register
     */
    public void registerProducer(String producersName, ProducerFunction<?> producer) {
        producers.putIfAbsent(producersName, producer);
    }

    /**
     * Return the bean property value converted to the requested type.
     *
     * @param beanName the logical bean name
     * @param propertyName the property suffix
     * @param type the target type for conversion
     * @return the converted value, or {@code null} if absent
     */
    public Object getBeanPropertyValue(String beanName, String propertyName, Type type) {
        ParameterizedType parameterizedType = null;
        Class<?> clazz;
        if (type instanceof ParameterizedType) {
            parameterizedType = (ParameterizedType) type;
            clazz = (Class<?>) parameterizedType.getRawType();
        } else {
            clazz = (Class<?>) type;
        }
        String stringValue = getBeanPropertyValue(beanName, propertyName);
        if (clazz == ProducerFunction.class && stringValue != null) {
            return producers.get(stringValue);
        }
        if (stringValue == null) return null;
        stringValue = stringValue.trim();
        try {
            return getObject(clazz, parameterizedType, stringValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported type for value conversion: " + type, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object getObject(Class<?> clazz, ParameterizedType parameterizedType, String stringValue)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (clazz == String.class) return stringValue;
        if (clazz == Duration.class) {
            String durationString = stringValue.toUpperCase();
            if (!durationString.startsWith("PT")) durationString = "PT" + durationString;
            return Duration.parse(durationString);
        }
        if (clazz == Integer.class || clazz == int.class) return Integer.valueOf(stringValue);
        if (clazz == Long.class || clazz == long.class) return Long.valueOf(stringValue);
        if (clazz == Boolean.class || clazz == boolean.class) return Boolean.valueOf(stringValue);
        if (clazz == Double.class || clazz == double.class) return Double.valueOf(stringValue);
        // Enum support
        if (clazz.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Class<? extends Enum> enumClass = (Class<? extends Enum<?>>) clazz;
            //noinspection unchecked
            return Enum.valueOf(enumClass, stringValue.substring(stringValue.lastIndexOf(".") + 1));
        }
        if (parameterizedType != null) {
            // Try to resolve generic parameter (e.g., List<SomeEnum>)
            List<Object> list = new ArrayList<>();
            Type arg = parameterizedType.getActualTypeArguments()[0];
            for (String val : stringValue.split(",")) {
                list.add(getObject((Class<?>) arg, null, val));
            }
            if (Set.class.isAssignableFrom(clazz)) return Set.copyOf(list);
            if (Collection.class.isAssignableFrom(clazz)) return list;
        }
        return clazz.getConstructor(String.class).newInstance(stringValue);
    }

    /**
     * Return the bean property value, resolving CDI lookups when the value starts with {@link #LOOKUP_PREFIX}.
     *
     * @param lookup CDI Instance for resolving beans
     * @param beanName the logical bean name
     * @param propertyName the property suffix
     * @param type the target type for conversion or lookup
     * @return the resolved value, or {@code null} if absent
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object getBeanPropertyValue(Instance<Object> lookup, String beanName, String propertyName, Type type) {
        String stringValue = getBeanPropertyValue(beanName, propertyName);
        if (stringValue == null) return null;

        List<Object> results = new ArrayList<>();
        String[] values = stringValue.split(",");
        for (String value : values) {
            String trimmedValue = value.trim();
            if (trimmedValue.startsWith(LOOKUP_PREFIX)) {
                String lookupableBean = trimmedValue.substring(LOOKUP_PREFIX.length());
                switch (lookupableBean) {
                    case "@default" -> {
                        Object result;
                        if (type instanceof ParameterizedType parameterizedType) {
                            result = selectByBeanManager(parameterizedType);
                        } else {
                            Instance instance = lookup.select((Class) type);
                            if (instance != null && instance.isResolvable()) {
                                result = instance.get();
                            } else {
                                result = selectByBeanManager(type);
                            }
                        }
                        if (result != null) {
                            results.add(result);
                        }
                    }
                    case "@all" -> {
                        if (type instanceof ParameterizedType pt) {
                            Type actualTypeArgument = pt.getActualTypeArguments()[0];
                            Stream<?> resultStream;
                            if (actualTypeArgument instanceof ParameterizedType parameterizedType) {
                                resultStream = selectAllByBeanManager(parameterizedType).stream();
                            } else {
                                Instance<?> instance = lookup.select((Class<?>) actualTypeArgument);
                                if (instance != null && instance.isResolvable()) {
                                    resultStream = instance.stream();
                                } else {
                                    resultStream = selectAllByBeanManager(actualTypeArgument).stream();
                                }
                            }
                            if (resultStream != null) {
                                resultStream.forEachOrdered(results::add);
                            }
                        } else throw new IllegalConfigurationException("Cannot use @all for non generic types");
                    }

                    default -> {
                        Class<?> resultType = type instanceof ParameterizedType pt
                                ? (Class<?>) pt.getActualTypeArguments()[0]
                                : (Class<?>) type;
                        Instance<?> resultInstance = getInstance(
                                lookup, resultType, lookupableBean.substring(lookupableBean.startsWith("@") ? 1 : 0));
                        if (resultInstance != null) results.add(resultInstance.get());
                    }
                }

            } else {
                Optional<Object> optionalValue = lookupObject(lookup, trimmedValue);
                optionalValue.ifPresent(results::add);
            }
        }

        if (results.isEmpty()) {
            Object result = getBeanPropertyValue(beanName, propertyName, type);
            if (result != null) {
                if (result instanceof Collection collection) results.addAll(collection);
                else results.add(result);
            }
        }

        if (type instanceof ParameterizedType pt) {
            Class<?> rawType = (Class<?>) pt.getRawType();
            if (Set.class.isAssignableFrom(rawType)) return Set.copyOf(results);
            if (Collection.class.isAssignableFrom(rawType)) return List.copyOf(results);
        }

        if (results.size() > 1) {
            throw new IllegalConfigurationException("We discovered " + results.size() + " objects for property bean '"
                    + propertyName + "' (class: " + beanName + ").");
        }

        return results.isEmpty() ? null : results.get(0);
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> lookupObject(Instance<Object> lookup, final String value) {
        try {
            Class<?> clazz = Class.forName(value);
            Instance<Object> object = (Instance<Object>) lookup.select(clazz);
            if (object != null && object.isResolvable()) return (Optional<T>) Optional.of(object.get());
            return (Optional<T>) Optional.of(clazz.getDeclaredConstructor().newInstance());
        } catch (ReflectiveOperationException e) {
            // TODO Auto-generated catch block
            return Optional.empty();
        }
    }

    private static java.util.function.Supplier<BeanManager> beanManagerSupplier =
            () -> CDI.current().getBeanManager();

    /**
     * For tests only: override how BeanManager is obtained.
     *
     * @param supplier the supplier to use, or {@code null} to reset to the default
     */
    public static void setBeanManagerSupplier(java.util.function.Supplier<BeanManager> supplier) {
        beanManagerSupplier = (supplier == null) ? () -> CDI.current().getBeanManager() : supplier;
    }

    private Object selectByBeanManager(Type type) {
        BeanManager bm = beanManagerSupplier.get();
        Set<Bean<?>> beans = bm.getBeans(type);
        if (beans.isEmpty()) {
            throw new IllegalConfigurationException("The type " + type + " is not found in the CDI container.");
        }
        Bean<?> bean = bm.resolve(beans);
        var ctx = bm.createCreationalContext(bean);
        return bm.getReference(bean, type, ctx);
    }

    private List<Object> selectAllByBeanManager(Type type) {
        BeanManager bm = beanManagerSupplier.get();
        Set<Bean<?>> beans = bm.getBeans(type);
        if (beans.isEmpty()) {
            throw new IllegalConfigurationException("The type " + type + " is not found in the CDI container.");
        }
        List<Object> beansList = new ArrayList<>();
        for (Bean<?> bean : beans) {
            var ctx = bm.createCreationalContext(bean);
            beansList.add(bm.getReference(bean, type, ctx));
        }
        return beansList;
    }

    private <T> Instance<T> getInstance(Instance<Object> lookup, Class<T> clazz, String lookupName) {
        if (lookupName == null || lookupName.isBlank()) return lookup.select(clazz);
        return lookup.select(clazz, NamedLiteral.of(lookupName));
    }

    /**
     * Return the configuration property names (without the common prefix) for the given bean.
     *
     * @param beanName the logical bean name
     * @return set of property name suffixes under {@code config.}
     */
    public Set<String> getPropertyNamesForBean(String beanName) {
        String configPrefix = PREFIX + "." + beanName + ".config.";
        return getPropertyKeys().stream()
                .map(Object::toString)
                .filter(prop -> prop.startsWith(configPrefix))
                .map(prop -> prop.substring(configPrefix.length()))
                .collect(Collectors.toSet());
    }
}
