package dev.langchain4j.cdi.core.config;

import dev.langchain4j.cdi.core.config.spi.LLMConfig;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test utility LLMConfig implementation that reads configuration from a Java 17 text block (multiline string).
 * Example usage:
 * String cfg = """
 * dev.langchain4j.plugin.content-retriever.class=dev.example.MyRetriever
 * dev.langchain4j.plugin.content-retriever.config.api-key=xyz
 * """;
 * LLMConfig config = new TestTextBlockLLMConfig(cfg);
 */
public class TextBlockLLMConfig extends LLMConfig {

    private final Properties properties = new Properties();
    private String textBlock;

    @SuppressWarnings("unused")
    public TextBlockLLMConfig() {
        this("");
    }

    public TextBlockLLMConfig(String textBlock) {
        this.textBlock = textBlock == null ? "" : textBlock;
    }

    public void reinitForTest(String textBlock) {
        this.textBlock = textBlock;
        init();
    }

    @Override
    public void init() {
        try (StringReader reader = new StringReader(textBlock)) {
            properties.clear();
            properties.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> getPropertyKeys() {
        return properties.keySet().stream().map(Object::toString).collect(Collectors.toSet());
    }

    @Override
    public String getValue(String key) {
        return properties.getProperty(key);
    }

}
