package dev.langchain4j.cdi.plugin;

public class DummyWithStringConstructor {
    private final String value;

    public DummyWithStringConstructor(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
