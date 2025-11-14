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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base abstraction to provide configuration for LLM-related beans.
 *
 * <p>This API is intentionally lightweight and relies purely on CDI (no external config dependency). It is inspired by
 * MicroProfile/SmallRye Config, but kept minimal for portability.
 */
public abstract class LLMConfig {
    Map<String, ProducerFunction<?>> producers = new ConcurrentHashMap<>();

    /** Prefix for all LLM beans properties. */
    public static final String PREFIX = "dev.langchain4j.plugin";

    /** Called by @see LLMConfigProvider. */
    public static final String PRODUCER = "defined_bean_producer";

    public static final String CLASS = "class";
    public static final String SCOPE = "scope";
    private static final Logger LOGGER = Logger.getLogger(LLMConfig.class.getName());

    public abstract void init();

    public abstract Set<String> getPropertyKeys();

    public abstract String getValue(String key);

    /**
     * Get all Langchain4j-cdi LLM beans names, prefixed by PREFIX For example:
     * dev.langchain4j.plugin.content-retriever.class -> content-retriever
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

    public String getBeanPropertyValue(String beanName, String propertyName) {
        String key = PREFIX + "." + beanName + "." + propertyName;
        return getValue(key);
    }

    public void registerProducer(String producersName, ProducerFunction<?> producer) {
        producers.putIfAbsent(producersName, producer);
    }

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object getBeanPropertyValue(Instance<Object> lookup, String beanName, String propertyName, Type type) {
        String stringValue = getBeanPropertyValue(beanName, propertyName);
        if (stringValue == null) return null;
        if (stringValue.startsWith("lookup:")) {
            String lookupableBean = stringValue.substring("lookup:".length());
            switch (lookupableBean) {
                case "@default" -> {
                    if (type instanceof ParameterizedType parameterizedType) {
                        return selectByBeanManager(parameterizedType);
                    }
                    return lookup.select((Class) type).get();
                }
                case "@all" -> {
                    if (type instanceof ParameterizedType pt) {
                        Type actualTypeArgument = pt.getActualTypeArguments()[0];
                        Stream<?> toReturn;
                        if (actualTypeArgument instanceof ParameterizedType parameterizedType) {
                            toReturn = selectAllByBeanManager(parameterizedType).stream();
                        } else {
                            toReturn = lookup.select((Class<?>) actualTypeArgument).stream();
                        }
                        if (List.class.equals(pt.getRawType())) {
                            return toReturn.toList();
                        }
                        if (pt.getRawType().equals(Set.class)) {
                            return toReturn.collect(Collectors.toSet());
                        }
                        throw new IllegalConfigurationException("@all can only be used with List or Set");
                    }
                    throw new IllegalConfigurationException("Cannot use @all for non generic types");
                }
                default -> {
                    Class<?> resultType = type instanceof ParameterizedType pt
                            ? (Class<?>) pt.getActualTypeArguments()[0]
                            : (Class<?>) type;
                    List<Object> results = new ArrayList<>();
                    String[] lookupNames = lookupableBean.split(",");
                    if (lookupNames != null && lookupNames.length > 0) {
                        for (String lookupName : lookupNames) {
                            String name = lookupName.trim();
                            results.add(getInstance(lookup, resultType, name.substring(name.startsWith("@") ? 1 : 0))
                                    .get());
                        }
                    } else
                        results.add(
                                getInstance(lookup, resultType, lookupableBean).get());

                    if (type instanceof ParameterizedType pt) {
                        Class<?> rawType = (Class<?>) pt.getRawType();
                        if (Collection.class.isAssignableFrom(rawType)) {
                            if (Set.class.isAssignableFrom(rawType)) return Set.copyOf(results);
                            return List.copyOf(results);
                        }
                    }

                    if (results.size() > 1) {
                        throw new IllegalConfigurationException("We discovered " + results.size()
                                + " objects for property bean '" + propertyName + "' (class: " + beanName + ").");
                    }

                    return results.get(0);
                }
            }
        } else {
            return getBeanPropertyValue(beanName, propertyName, type);
        }
    }

    private static java.util.function.Supplier<BeanManager> beanManagerSupplier =
            () -> CDI.current().getBeanManager();

    /** For tests only: override how BeanManager is obtained. */
    public static void setBeanManagerSupplier(java.util.function.Supplier<BeanManager> supplier) {
        beanManagerSupplier = (supplier == null) ? () -> CDI.current().getBeanManager() : supplier;
    }

    private Object selectByBeanManager(ParameterizedType type) {
        BeanManager bm = beanManagerSupplier.get();
        Set<Bean<?>> beans = bm.getBeans(type);
        if (beans.isEmpty()) {
            throw new IllegalConfigurationException("The type " + type + " is not found in the CDI container.");
        }
        Bean<?> bean = bm.resolve(beans);
        var ctx = bm.createCreationalContext(bean);
        return bm.getReference(bean, type, ctx);
    }

    private List<Object> selectAllByBeanManager(ParameterizedType type) {
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

    public Set<String> getPropertyNamesForBean(String beanName) {
        String configPrefix = PREFIX + "." + beanName + ".config.";
        return getPropertyKeys().stream()
                .map(Object::toString)
                .filter(prop -> prop.startsWith(configPrefix))
                .map(prop -> prop.substring(configPrefix.length()))
                .collect(Collectors.toSet());
    }
}
