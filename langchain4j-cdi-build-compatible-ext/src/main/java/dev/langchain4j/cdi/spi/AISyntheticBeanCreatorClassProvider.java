/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package dev.langchain4j.cdi.spi;

import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Resolves the {@link SyntheticBeanCreator} class to use for LLM plugin beans by loading
 * {@link AISyntheticBeanCreatorClassFactory} implementations via {@link ServiceLoader} and selecting the
 * highest-priority one.
 */
public class AISyntheticBeanCreatorClassProvider {

    /** Creates a new {@code AISyntheticBeanCreatorClassProvider}. */
    public AISyntheticBeanCreatorClassProvider() {}

    private static final AISyntheticBeanCreatorClassFactory factory;

    static {
        ServiceLoader<AISyntheticBeanCreatorClassFactory> loader = ServiceLoader.load(
                AISyntheticBeanCreatorClassFactory.class, Thread.currentThread().getContextClassLoader());
        final List<AISyntheticBeanCreatorClassFactory> factories = new ArrayList<>();
        loader.forEach(factories::add);
        if (factories.isEmpty()) {
            factory = new AISyntheticBeanCreatorClassFactory() {
                @Override
                public Class<? extends SyntheticBeanCreator<Object>> getSyntheticBeanCreatorClass() {
                    try {
                        return (Class<? extends SyntheticBeanCreator<Object>>) Thread.currentThread()
                                .getContextClassLoader()
                                .loadClass("dev.langchain4j.cdi.plugin.LLMPluginCreator");
                    } catch (ClassNotFoundException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public int getPriority() {
                    return -1;
                }
            };
        } else {
            Collections.sort(factories);
            factory = factories.get(factories.size() - 1);
        }
    }

    /**
     * Returns the {@link SyntheticBeanCreator} class from the highest-priority factory.
     *
     * @return the synthetic bean creator class
     */
    public static Class<? extends SyntheticBeanCreator<Object>> getSyntheticBeanCreatorClass() {
        return factory.getSyntheticBeanCreatorClass();
    }
}
