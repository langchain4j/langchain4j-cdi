package dev.langchain4j.cdi.example.booking;

import dev.langchain4j.cdi.core.config.spi.LLMConfig;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class DummyLLConfig extends LLMConfig {
    Properties properties = new Properties();

    @Override
    public void init() {
        try (FileReader fileReader = new FileReader(System.getProperty("llmconfigfile"))) {
            properties.load(fileReader);

            // Workaround for https://github.com/langchain4j/langchain4j-cdi/issues/144
            // CommonLLMPluginCreator calls llmConfig.getBeanPropertyValue(beanName, LLMConfig.PRODUCER, ProducerFunction.class)
            // to find out a ProducerFunction. If found, invokes this ProducerFunction to build the bean
            String beanProducer =getValue(PREFIX + ".docRagRetriever.defined_bean_producer");
            if (beanProducer == null) {
                return;
            }
            registerProducer(beanProducer, (Instance<Object> ctx, String beanName, LLMConfig cfg) -> {

                // Resolve using BeanManager
                BeanManager bm = CDI.current().getBeanManager();

                // Resolve EmbeddingModel
                EmbeddingModel model;
                {
                    java.util.Set<Bean<?>> beans = bm.getBeans(EmbeddingModel.class);
                    if (beans == null || beans.isEmpty()) {
                        beans = bm.getBeans(EmbeddingModel.class);
                    }
                    Bean<?> bean = bm.resolve(beans);
                    var creationalContext = bm.createCreationalContext(bean);
                    model = (EmbeddingModel) bm.getReference(bean, EmbeddingModel.class, creationalContext);
                }

                // Resolve EmbeddingStore<TextSegment>
                EmbeddingStore<TextSegment> store;
                {
                    java.lang.reflect.Type storeType = new TypeLiteral<EmbeddingStore<TextSegment>>() {}.getType();
                    java.util.Set<Bean<?>> beans = bm.getBeans(storeType);
                    if (beans == null || beans.isEmpty()) {
                        beans = bm.getBeans(storeType);
                    }
                    Bean<?> bean = bm.resolve(beans);
                    var creationalContext = bm.createCreationalContext(bean);
                    @SuppressWarnings("unchecked")
                    EmbeddingStore<TextSegment> resolved = (EmbeddingStore<TextSegment>) bm.getReference(bean, storeType, creationalContext);
                    store = resolved;
                }

                try {
                    Class<?> retrieverClass = Class.forName("dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever");
                    Object builder = retrieverClass.getMethod("builder").invoke(null);
                    Class<?> builderClass = builder.getClass();

                    builderClass.getMethod("embeddingModel", EmbeddingModel.class).invoke(builder, model);
                    Class<?> embStoreIface = Class.forName("dev.langchain4j.store.embedding.EmbeddingStore");
                    builderClass.getMethod("embeddingStore", embStoreIface).invoke(builder, store);

                    try {
                        String maxResults = cfg.getBeanPropertyValue("docRagRetriever", "config.maxResults");
                        if (maxResults != null && !maxResults.isBlank()) {
                            Method method = Arrays.stream(builderClass.getMethods())
                                    .filter(m -> m.getName().equals("maxResults"))
                                    .filter(m -> m.getParameterCount() == 1)
                                    .filter(m -> m.getParameterTypes()[0] == int.class
                                            || m.getParameterTypes()[0] == Integer.class)
                                    .findFirst()
                                    .orElseThrow(() -> new NoSuchMethodException("No suitable maxResults method"));

                            int value = Integer.parseInt(maxResults.trim());
                            method.invoke(builder, value);
                        }
                    } catch (Exception ignoreExc) {
                        // Ignore exception
                    }
                    try {
                        String minScore = cfg.getBeanPropertyValue("docRagRetriever", "config.minScore");
                        if (minScore != null && !minScore.isBlank()) {
                            Method method = Arrays.stream(builderClass.getMethods())
                                    .filter(m -> m.getName().equals("minScore"))
                                    .filter(m -> m.getParameterCount() == 1)
                                    .filter(m -> m.getParameterTypes()[0] == double.class
                                            || m.getParameterTypes()[0] == Double.class)
                                    .findFirst()
                                    .orElseThrow(() -> new NoSuchMethodException("No suitable minScore method"));

                            int value = Integer.parseInt(minScore.trim());
                            method.invoke(builder, value);
                        }
                    } catch (Exception ignoreExc) {
                        // Ignore exception
                    }
                    return builderClass.getMethod("build").invoke(builder);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to build EmbeddingStoreContentRetriever via reflection", e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> getPropertyKeys() {
        return properties.keySet().stream()
                .map(Object::toString)
                .filter(prop -> prop.startsWith(PREFIX))
                .collect(Collectors.toSet());
    }

    @Override
    public String getValue(String key) {
        return properties.getProperty(key);
    }
}
