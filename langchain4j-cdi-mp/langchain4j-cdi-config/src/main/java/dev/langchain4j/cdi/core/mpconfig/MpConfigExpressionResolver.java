package dev.langchain4j.cdi.core.mpconfig;

import dev.langchain4j.cdi.spi.ExpressionResolver;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * ExpressionResolver that resolves {@code ${property.key}} expressions via MicroProfile Config.
 *
 * <p>When a value matches the {@code ${...}} pattern, the enclosed string is used as a MicroProfile Config property
 * key. Resolution outcomes:
 *
 * <ul>
 *   <li><b>Key found</b> — the config value is returned.
 *   <li><b>Key not found</b> — the original {@code ${...}} expression is returned unchanged (logged at FINE) so the
 *       missing property is visible in downstream error messages rather than silently replaced with {@code null}.
 *   <li><b>MP Config unavailable</b> ({@link IllegalStateException} from {@link ConfigProvider#getConfig()}) — the
 *       original expression is returned unchanged and a WARNING is logged. This happens when the module is on the
 *       classpath but no {@code ConfigProviderResolver} implementation is registered (e.g. a non-MP runtime).
 *   <li><b>Any other exception</b> — propagated as-is. These indicate a broken config source and should surface at
 *       deployment time rather than being silently swallowed.
 * </ul>
 *
 * <p>Registered automatically via {@link java.util.ServiceLoader} when this module is on the classpath.
 */
public class MpConfigExpressionResolver implements ExpressionResolver {

    /** Creates a new {@code MpConfigExpressionResolver}. */
    public MpConfigExpressionResolver() {}

    private static final Logger LOGGER = Logger.getLogger(MpConfigExpressionResolver.class.getName());
    private static final Pattern MP_CONFIG_PATTERN = Pattern.compile("^\\$\\{(.+)\\}$");

    @Override
    public String resolve(String value) {
        Matcher matcher = MP_CONFIG_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return value;
        }
        String key = matcher.group(1);
        try {
            return ConfigProvider.getConfig()
                    .getOptionalValue(key, String.class)
                    .orElseGet(() -> {
                        LOGGER.log(
                                Level.FINE,
                                "No MicroProfile Config value found for key ''{0}'', keeping original expression",
                                key);
                        return value;
                    });
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING, "MicroProfile Config is not available, cannot resolve '${" + key + "}'", e);
            return value;
        }
    }
}
