package dev.langchain4j.cdi.plugin;

import java.util.List;

public class DummyModel extends DummyBaseModel {
    final String param1;
    final DummyEnum dummyEnum;
    final List<DummyEnum> dummyEnumList;
    final DummyWithStringConstructor dummyWithStringConstructor;

    DummyModel(String apiKey, Integer timeout, DummyInjected dummyInjected, DummyParam<Integer> dummyParamInt, String param1,
            DummyEnum dummyEnum,
            List<DummyEnum> dummyEnumList, DummyWithStringConstructor dummyWithStringConstructor) {
        super(apiKey, timeout, dummyInjected, dummyParamInt);
        this.param1 = param1;
        this.dummyEnum = dummyEnum;
        this.dummyEnumList = dummyEnumList;
        this.dummyWithStringConstructor = dummyWithStringConstructor;
    }

    public static DummyModelBuilder builder() {
        return new DummyModelBuilder();
    }

    public static class DummyModelBuilder extends Builder<DummyModel, DummyModel.DummyModelBuilder> {
        public String param1;
        public DummyEnum dummyEnum;
        public List<DummyEnum> dummyEnumList;
        public DummyWithStringConstructor dummyWithStringConstructor;

        public DummyModelBuilder param1(String v) {
            this.param1 = v;
            return this;
        }

        public DummyModelBuilder dummyEnum(DummyEnum v) {
            this.dummyEnum = v;
            return this;
        }

        public DummyModelBuilder dummyEnumList(java.util.List<DummyEnum> v) {
            this.dummyEnumList = v;
            return this;
        }

        public DummyModelBuilder dummyWithStringConstructor(DummyWithStringConstructor v) {
            this.dummyWithStringConstructor = v;
            return this;
        }

        public DummyModel build() {
            return new DummyModel(apiKey, timeout, dummyInjected, dummyParamInt, param1, dummyEnum, dummyEnumList,
                    dummyWithStringConstructor);
        }
    }
}
