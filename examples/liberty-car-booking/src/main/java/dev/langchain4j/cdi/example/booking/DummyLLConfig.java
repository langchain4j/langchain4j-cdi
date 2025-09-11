package dev.langchain4j.cdi.example.booking;

import dev.langchain4j.cdi.core.config.spi.LLMConfig;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class DummyLLConfig extends LLMConfig {
    Properties properties = new Properties();

    @Override
    public void init() {
        try (FileReader fileReader = new FileReader(System.getProperty("llmconfigfile"))) {
            properties.load(fileReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
