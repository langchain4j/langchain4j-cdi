package dev.langchain4j.cdi.core.integrationtests;

import dev.langchain4j.cdi.core.config.spi.LLMConfig;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DummyLLConfig extends LLMConfig {
    Properties properties = new Properties();
    private static final Logger LOGGER = Logger.getLogger(DummyLLConfig.class.getName());

    @Override
    public void init() {
        LOGGER.info("Initializing Dummy LLConfig");
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("llm-config.properties")) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new UndeclaredThrowableException(e);
        }
        properties.keySet().forEach(key -> {
            LOGGER.info("Key: " + key);
        });
    }

    @Override
    public Set<String> getPropertyKeys() {
        return properties.keySet().stream().map(Object::toString)
                .filter(prop -> prop.startsWith(PREFIX))
                .collect(Collectors.toSet());
    }

    @Override
    public String getValue(String key) {
        return properties.getProperty(key);
    }
}
