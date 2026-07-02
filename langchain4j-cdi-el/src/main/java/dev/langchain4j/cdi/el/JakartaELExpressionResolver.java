package dev.langchain4j.cdi.el;

import dev.langchain4j.cdi.spi.ExpressionResolver;
import jakarta.el.ELProcessor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ExpressionResolver that evaluates Jakarta EE Expression Language expressions enclosed in {@code #{...}} delimiters.
 *
 * <p>When a value matches the {@code #{...}} pattern, the enclosed string is evaluated as a Jakarta EL expression. CDI
 * bean resolution is wired automatically when CDI is active at resolution time: the {@code BeanManager}'s ELResolver is
 * added to the {@link ELProcessor} so that {@code @Named} beans can be referenced directly, for example
 * {@code #{myService.modelName}}.
 *
 * <p>If CDI is not active (e.g. during unit tests), basic EL evaluation still works: arithmetic, string operations,
 * system properties via {@code #{systemProperties['key']}}, etc.
 *
 * <p>If evaluation fails for any reason the original {@code #{...}} expression is returned unchanged and a WARNING is
 * logged so the problem is visible rather than silently swallowed.
 *
 * <p>Registered automatically via {@link java.util.ServiceLoader} when this module is on the classpath.
 */
public class JakartaELExpressionResolver implements ExpressionResolver {

    private static final Logger LOGGER = Logger.getLogger(JakartaELExpressionResolver.class.getName());
    private static final Pattern EL_PATTERN = Pattern.compile("^#\\{(.+)\\}$");

    /** Creates a new {@code JakartaELExpressionResolver}. */
    public JakartaELExpressionResolver() {}

    @Override
    public String resolve(String value) {
        Matcher matcher = EL_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return value;
        }
        String expression = matcher.group(1);
        try {
            Object result = newProcessor().eval(expression);
            return result != null ? result.toString() : value;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to resolve Jakarta EL expression ''{0}'': {1}", new Object[] {
                value, e.getMessage()
            });
            return value;
        }
    }

    private static ELProcessor newProcessor() {
        ELProcessor elp = new ELProcessor();
        try {
            jakarta.enterprise.inject.spi.BeanManager bm =
                    jakarta.enterprise.inject.spi.CDI.current().getBeanManager();
            elp.getELManager().addELResolver(bm.getELResolver());
        } catch (IllegalStateException ignored) {
            // CDI not active — basic EL evaluation proceeds without bean resolution
        }
        return elp;
    }
}
