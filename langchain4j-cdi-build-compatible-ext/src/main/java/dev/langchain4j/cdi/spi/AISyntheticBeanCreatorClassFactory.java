/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package dev.langchain4j.cdi.spi;

import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;

/**
 * SPI for providing a custom {@link SyntheticBeanCreator} class used to instantiate LLM plugin beans. Implementations
 * are discovered via {@link java.util.ServiceLoader} and the highest-priority factory is selected.
 */
public interface AISyntheticBeanCreatorClassFactory extends Comparable<AISyntheticBeanCreatorClassFactory> {

    /**
     * Returns the {@link SyntheticBeanCreator} class to use for creating LLM plugin beans.
     *
     * @return the synthetic bean creator class
     */
    Class<? extends SyntheticBeanCreator<Object>> getSyntheticBeanCreatorClass();

    /**
     * The priority for the BeanCreatorFactory when resolving the service to get the implementation. This is used when
     * selecting the implementation when several implementations are loaded. The highest priority implementation will be
     * used.
     *
     * @return the priority.
     */
    int getPriority();

    @Override
    default int compareTo(AISyntheticBeanCreatorClassFactory other) {
        return this.getPriority() - other.getPriority();
    }
}
