package dev.langchain4j.cdi.core.mpconfig;

import dev.langchain4j.cdi.core.config.spi.LLMConfig;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * {@link LLMConfig} implementation backed by MicroProfile Config.
 *
 * <p>Reads LangChain4j plugin configuration properties from the MicroProfile Config subsystem, filtering on the
 * standard {@value LLMConfig#PREFIX} prefix.
 */
public class LLMConfigMPConfig extends LLMConfig {

    /** Creates a new {@code LLMConfigMPConfig}. */
    public LLMConfigMPConfig() {}

    private Config config;

    @Override
    public void init() {
        config = ConfigProvider.getConfig();
    }

    private static Stream<String> getPropertyNameStream(Config config) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(config.getPropertyNames().iterator(), Spliterator.ORDERED), false);
    }

    @Override
    public Set<String> getPropertyKeys() {
        return getPropertyNameStream(config)
                .filter(prop -> prop.startsWith(PREFIX))
                .collect(Collectors.toSet());
    }

    @Override
    public String getValue(String key) {
        return config.getOptionalValue(key, String.class).orElse(null);
    }
}
