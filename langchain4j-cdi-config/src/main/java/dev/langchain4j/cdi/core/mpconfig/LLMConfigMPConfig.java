package dev.langchain4j.cdi.core.mpconfig;

import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import dev.langchain4j.cdi.core.config.spi.LLMConfig;

public class LLMConfigMPConfig extends LLMConfig {

    private Config config;

    @Override
    public void init() {
        config = ConfigProvider.getConfig();
    }

    private static Stream<String> getPropertyNameStream(Config config) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(config.getPropertyNames().iterator(), Spliterator.ORDERED),
                false);
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
