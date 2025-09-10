package dev.langchain4j.cdi.plugin;

// Based on Langchain4J examples
abstract class DummyBaseModel {
    final String apiKey;
    final Integer timeout;
    final DummyInjected dummyInjected;
    final DummyParam<Integer> dummyParamInt;

    DummyBaseModel(String apiKey, Integer timeout, DummyInjected dummyInjected, DummyParam<Integer> dummyParamInt) {
        this.apiKey = apiKey;
        this.timeout = timeout;
        this.dummyInjected = dummyInjected;
        this.dummyParamInt = dummyParamInt;
    }

    protected abstract static class Builder<C extends DummyBaseModel, B extends Builder<C, B>> {
        // fields that mirror expected property names (after dashToCamel conversion)
        public String apiKey;
        public Integer timeout;
        public DummyInjected dummyInjected;
        public DummyParam<Integer> dummyParamInt;

        public B apiKey(String v) {
            this.apiKey = v;
            return (B) this;
        }

        public B timeout(Integer v) {
            this.timeout = v;
            return (B) this;
        }

        public B dummyInjected(DummyInjected v) {
            this.dummyInjected = v;
            return (B) this;
        }

        public B dummyParamInt(DummyParam<Integer> v) {
            this.dummyParamInt = v;
            return (B) this;
        }

        public abstract C build();
    }
}
