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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Base abstraction to provide configuration for LLM-related beans.
 * <p>
 * This API is intentionally lightweight and relies purely on CDI (no external config dependency). It is inspired by
 * MicroProfile/SmallRye Config, but kept minimal for portability.
 */
public abstract class LLMConfig {
    Map<String, ProducerFunction<?>> producers = new ConcurrentHashMap<>();

    /**
     * Prefix for all LLM beans properties.
     */
    public static final String PREFIX = "dev.langchain4j.plugin";

    /**
     * Called by @see LLMConfigProvider.
     */
    public static final String PRODUCER = "defined_bean_producer";
    public static final String CLASS = "class";
    public static final String SCOPE = "scope";

    public abstract void init();

    public abstract Set<String> getPropertyKeys();

    public abstract String getValue(String key);

    /**
     * Get all Langchain4j-cdi LLM beans names, prefixed by PREFIX
     * For example: dev.langchain4j.plugin.content-retriever.class -> content-retriever
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
        if (stringValue == null)
            return null;
        stringValue = stringValue.trim();
        try {
            return getObject(clazz, parameterizedType, stringValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported type for value conversion: " + type, e);
        }

    }

    private Object getObject(Class<?> clazz, ParameterizedType parameterizedType, String stringValue)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (clazz == String.class)
            return stringValue;
        if (clazz == Duration.class)
            return Duration.parse(stringValue);
        if (clazz == Integer.class || clazz == int.class)
            return Integer.valueOf(stringValue);
        if (clazz == Long.class || clazz == long.class)
            return Long.valueOf(stringValue);
        if (clazz == Boolean.class || clazz == boolean.class)
            return Boolean.valueOf(stringValue);
        if (clazz == Double.class || clazz == double.class)
            return Double.valueOf(stringValue);
        // Enum support
        if (clazz.isEnum()) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Class<? extends Enum> enumClass = (Class<? extends Enum<?>>) clazz;
            //noinspection unchecked
            return Enum.valueOf(enumClass, stringValue);
        }
        if (clazz.isAssignableFrom(List.class) && parameterizedType != null) {
            // Try to resolve generic parameter (e.g., List<SomeEnum>)
            List<Object> list = new ArrayList<>();
            Type arg = parameterizedType.getActualTypeArguments()[0];
            for (String val : stringValue.split(",")) {
                list.add(getObject((Class<?>) arg, null, val));
            }
            return list;
        }
        return clazz.getConstructor(String.class).newInstance(stringValue);
    }

    @SuppressWarnings("unchecked")
    public Object getBeanPropertyValue(Instance<Object> lookup, String beanName, String propertyName, Type type) {
        String stringValue = getBeanPropertyValue(beanName, propertyName);
        if (stringValue == null)
            return null;

        if (stringValue.startsWith("lookup:")) {
            String lookupableBean = stringValue.substring("lookup:".length());
            Instance<?> inst;
            if ("@default".equals(lookupableBean)) {
                if (type instanceof ParameterizedType)
                    return selectByBeanManager((ParameterizedType) type);
                else
                    inst = lookup.select((Class) type);
            } else if ("@all".equals(lookupableBean)) {
                return lookup.select((Class<?>) type).stream().toList();
            } else {
                inst = getInstance(lookup, (Class<?>) type, lookupableBean);
            }
            return inst.get();
        } else {
            return getBeanPropertyValue(beanName, propertyName, type);
        }
    }

    private static java.util.function.Supplier<BeanManager> beanManagerSupplier = () -> CDI.current().getBeanManager();

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

    private <T> Instance<T> getInstance(Instance<Object> lookup, Class<T> clazz, String lookupName) {
        if (lookupName == null || lookupName.isBlank())
            return lookup.select(clazz);
        return lookup.select(clazz, NamedLiteral.of(lookupName));
    }

    public Set<String> getPropertyNamesForBean(String beanName) {
        String configPrefix = PREFIX + "." + beanName + ".config.";
        return getPropertyKeys().stream().map(Object::toString)
                .filter(prop -> prop.startsWith(configPrefix))
                .map(prop -> prop.substring(configPrefix.length()))
                .collect(Collectors.toSet());
    }
}
