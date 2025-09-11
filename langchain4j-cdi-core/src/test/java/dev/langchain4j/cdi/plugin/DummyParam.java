package dev.langchain4j.cdi.plugin;

import jakarta.enterprise.context.ApplicationScoped;

/** A simple generic bean used to test lookup:@default resolution with ParameterizedType. */
public interface DummyParam<T> {
    T value();
}

@ApplicationScoped
class DummyStringParam implements DummyParam<String> {
    @Override
    public String value() {
        return "S";
    }
}

@ApplicationScoped
class DummyIntegerParam implements DummyParam<Integer> {
    @Override
    public Integer value() {
        return 1;
    }
}
