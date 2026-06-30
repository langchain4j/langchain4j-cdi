package dev.langchain4j.cdi.aiservice;

import dev.langchain4j.cdi.spi.ExpressionResolver;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared CDI bean resolution utilities used by both {@link CommonAIServiceCreator} and
 * {@link dev.langchain4j.cdi.agent.CommonAgentCreator}.
 */
public final class CdiLookupHelper {

    private static final Logger LOGGER = Logger.getLogger(CdiLookupHelper.class.getName());
    private static final String DEFAULT_BEAN_NAME = "#default";
    private static final List<ExpressionResolver> EXPRESSION_RESOLVERS;

    static {
        List<ExpressionResolver> resolvers = new ArrayList<>();
        ServiceLoader.load(ExpressionResolver.class).forEach(resolvers::add);
        EXPRESSION_RESOLVERS = Collections.unmodifiableList(resolvers);
    }

    private CdiLookupHelper() {}

    /**
     * Returns {@code true} when {@code s} is non-null and contains at least one non-whitespace character.
     *
     * @param s the string to test
     * @return {@code true} if the string is non-null and non-blank
     */
    public static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Resolves any expression embedded in {@code value} by applying all {@link ExpressionResolver} implementations
     * discovered via {@link ServiceLoader}, in discovery order. Returns {@code value} unchanged when no resolvers are
     * registered or the value is blank/null.
     *
     * @param value the raw annotation attribute value, possibly containing expressions
     * @return the resolved value, or the original value if no resolvers matched
     */
    public static String resolveExpression(String value) {
        if (!hasText(value)) {
            return value;
        }
        String resolved = value;
        for (ExpressionResolver resolver : EXPRESSION_RESOLVERS) {
            resolved = resolver.resolve(resolved);
        }
        return resolved;
    }

    /**
     * Resolve a CDI Instance for the given type and name. If name is {@code "#default"}, select the default bean of the
     * given type. If name is blank or null, returns null (meaning: do not attempt to resolve). Expression patterns in
     * {@code name} are expanded via {@link #resolveExpression(String)} before lookup.
     *
     * @param <X> the bean type
     * @param lookup the CDI {@link Instance} used for bean resolution
     * @param type the expected bean class
     * @param name the bean name, {@code "#default"}, or an expression to resolve
     * @return the matching {@link Instance}, or {@code null} if {@code name} is blank
     */
    public static <X> Instance<X> getInstance(Instance<Object> lookup, Class<X> type, String name) {
        String resolved = resolveExpression(name);
        if (hasText(resolved)) {
            if (DEFAULT_BEAN_NAME.equals(resolved)) {
                LOGGER.fine(() -> "Resolving default " + type.getSimpleName() + " bean");
                return lookup.select(type);
            }
            LOGGER.fine(() -> "Resolving " + type.getSimpleName() + " bean named '" + resolved + "'");
            return lookup.select(type, NamedLiteral.of(resolved));
        }
        return null;
    }

    /**
     * Resolve a single bean instance by type and name, returning null if unresolvable. Convenience wrapper around
     * {@link #getInstance} that extracts the bean.
     *
     * @param <X> the bean type
     * @param lookup the CDI {@link Instance} used for bean resolution
     * @param type the expected bean class
     * @param name the bean name or expression to resolve
     * @return the resolved bean instance, or {@code null} if not resolvable
     */
    public static <X> X resolveSingle(Instance<Object> lookup, Class<X> type, String name) {
        Instance<X> instance = getInstance(lookup, type, name);
        if (instance != null && instance.isResolvable()) {
            return instance.get();
        }
        return null;
    }

    /**
     * Resolve guardrail instances by class. For each class, first attempts CDI lookup; if the bean is not resolvable,
     * falls back to instantiation via the no-arg constructor. Classes that fail both resolution paths are skipped with
     * a WARNING log.
     *
     * @param <G> the guardrail type
     * @param lookup the CDI {@link Instance} used for bean resolution
     * @param guardrailClasses the guardrail classes to resolve
     * @return list of resolved guardrail instances
     */
    public static <G> List<G> resolveGuardrailsByClass(Instance<Object> lookup, Class<? extends G>[] guardrailClasses) {
        List<G> guardrails = new ArrayList<>(guardrailClasses.length);
        for (Class<? extends G> guardrailClass : guardrailClasses) {
            try {
                Instance<? extends G> guardrailInstance = lookup.select(guardrailClass);
                if (guardrailInstance != null && guardrailInstance.isResolvable()) {
                    guardrails.add(guardrailInstance.get());
                } else {
                    guardrails.add(
                            guardrailClass.getConstructor((Class<?>[]) null).newInstance((Object[]) null));
                }
            } catch (ReflectiveOperationException | IllegalArgumentException ex) {
                LOGGER.log(
                        Level.WARNING,
                        "Failed to create guardrail " + guardrailClass + ", skipping: " + ex.getMessage(),
                        ex);
            }
        }
        return guardrails;
    }

    /**
     * Resolve guardrail instances by named CDI bean lookup. Names that cannot be resolved are skipped with a WARNING
     * log.
     *
     * @param <G> the guardrail type
     * @param lookup the CDI {@link Instance} used for bean resolution
     * @param type the guardrail class to select
     * @param names the CDI bean names to look up
     * @return list of resolved guardrail instances
     */
    public static <G> List<G> resolveGuardrailsByName(Instance<Object> lookup, Class<G> type, String[] names) {
        List<G> guardrails = new ArrayList<>(names.length);
        for (String name : names) {
            try {
                Instance<G> guardrailInstance = getInstance(lookup, type, name);
                if (guardrailInstance != null && guardrailInstance.isResolvable()) {
                    guardrails.add(guardrailInstance.get());
                } else {
                    LOGGER.log(Level.WARNING, "Named guardrail ''{0}'' is not resolvable, skipping", name);
                }
            } catch (IllegalArgumentException ex) {
                LOGGER.log(
                        Level.WARNING, "Failed to resolve guardrail '" + name + "', skipping: " + ex.getMessage(), ex);
            }
        }
        return guardrails;
    }

    /**
     * Resolve tool instances by named CDI bean lookup. Names that cannot be resolved are skipped with a WARNING log.
     *
     * @param toolNames the CDI bean names of the tools to resolve
     * @param lookup the CDI {@link Instance} used for bean resolution
     * @return list of resolved tool instances
     */
    public static List<Object> resolveToolsByName(String[] toolNames, Instance<Object> lookup) {
        List<Object> tools = new ArrayList<>(toolNames.length);
        for (String name : toolNames) {
            String resolved = resolveExpression(name);
            if (!hasText(resolved)) {
                continue;
            }
            Instance<Object> instance = lookup.select(Object.class, NamedLiteral.of(resolved));
            if (instance.isResolvable()) {
                tools.add(instance.get());
            } else {
                LOGGER.log(Level.WARNING, "Named tool ''{0}'' is not resolvable, skipping", resolved);
            }
        }
        return tools;
    }

    /**
     * Resolve tool instances from an array of tool classes. For each class, first attempts CDI lookup; if the bean is
     * not resolvable, falls back to instantiation via the no-arg constructor. Classes that fail both resolution paths
     * are skipped with a SEVERE log.
     *
     * @param toolClasses the tool classes to resolve or instantiate
     * @param lookup the CDI {@link Instance} used for bean resolution
     * @return list of resolved tool instances
     */
    public static List<Object> resolveToolInstances(Class<?>[] toolClasses, Instance<Object> lookup) {
        List<Object> tools = new ArrayList<>(toolClasses.length);
        for (Class<?> toolClass : toolClasses) {
            try {
                Instance<?> toolInstance = lookup.select(toolClass);
                if (toolInstance != null && toolInstance.isResolvable()) {
                    tools.add(toolInstance.get());
                } else {
                    tools.add(toolClass.getConstructor((Class<?>[]) null).newInstance((Object[]) null));
                }
            } catch (ReflectiveOperationException | IllegalArgumentException ex) {
                LOGGER.log(Level.SEVERE, "Failed to create tool " + toolClass + ", skipping: " + ex.getMessage(), ex);
            }
        }
        return tools;
    }

    /**
     * Resolve input guardrails using class-vs-name precedence. When both {@code guardrailClasses} and
     * {@code guardrailNames} are non-empty, classes take precedence and a WARNING is logged including
     * {@code interfaceName} so operators can identify the offending service.
     *
     * @param lookup the CDI {@link Instance} used for bean resolution
     * @param guardrailClasses the input guardrail classes (takes precedence over names)
     * @param guardrailNames the CDI bean names of input guardrails
     * @param interfaceName the AI service interface name, used in warning messages
     * @return list of resolved input guardrail instances
     */
    public static List<InputGuardrail> resolveInputGuardrails(
            Instance<Object> lookup,
            Class<? extends InputGuardrail>[] guardrailClasses,
            String[] guardrailNames,
            String interfaceName) {
        if (guardrailClasses.length > 0 && guardrailNames.length > 0) {
            LOGGER.log(
                    Level.WARNING,
                    "Both inputGuardrails and inputGuardrailNames specified for {0}. Using inputGuardrails classes and ignoring inputGuardrailNames.",
                    interfaceName);
        }
        if (guardrailClasses.length > 0) {
            return resolveGuardrailsByClass(lookup, guardrailClasses);
        } else if (guardrailNames.length > 0) {
            return resolveGuardrailsByName(lookup, InputGuardrail.class, guardrailNames);
        }
        return List.of();
    }

    /**
     * Resolve output guardrails using class-vs-name precedence. When both {@code guardrailClasses} and
     * {@code guardrailNames} are non-empty, classes take precedence and a WARNING is logged including
     * {@code interfaceName} so operators can identify the offending service.
     *
     * @param lookup the CDI {@link Instance} used for bean resolution
     * @param guardrailClasses the output guardrail classes (takes precedence over names)
     * @param guardrailNames the CDI bean names of output guardrails
     * @param interfaceName the AI service interface name, used in warning messages
     * @return list of resolved output guardrail instances
     */
    public static List<OutputGuardrail> resolveOutputGuardrails(
            Instance<Object> lookup,
            Class<? extends OutputGuardrail>[] guardrailClasses,
            String[] guardrailNames,
            String interfaceName) {
        if (guardrailClasses.length > 0 && guardrailNames.length > 0) {
            LOGGER.log(
                    Level.WARNING,
                    "Both outputGuardrails and outputGuardrailNames specified for {0}. Using outputGuardrails classes and ignoring outputGuardrailNames.",
                    interfaceName);
        }
        if (guardrailClasses.length > 0) {
            return resolveGuardrailsByClass(lookup, guardrailClasses);
        } else if (guardrailNames.length > 0) {
            return resolveGuardrailsByName(lookup, OutputGuardrail.class, guardrailNames);
        }
        return List.of();
    }
}
