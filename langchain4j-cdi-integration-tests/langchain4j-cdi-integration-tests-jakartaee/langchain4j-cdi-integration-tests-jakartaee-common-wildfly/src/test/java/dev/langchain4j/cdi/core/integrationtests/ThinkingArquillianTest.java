package dev.langchain4j.cdi.core.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.cdi.integrationtests.ListenerChatAiService;
import dev.langchain4j.cdi.integrationtests.ThinkingChatAiService;
import dev.langchain4j.cdi.integrationtests.ThinkingChatModelMock;
import dev.langchain4j.cdi.integrationtests.ThinkingChatRestService;
import dev.langchain4j.cdi.integrationtests.ThinkingListener;
import dev.langchain4j.cdi.integrationtests.ThinkingTestHelper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "resource"})
@ExtendWith(ArquillianExtension.class)
public class ThinkingArquillianTest {

    @SuppressWarnings("unused")
    @Deployment
    public static WebArchive createDeployment() throws IOException {
        File langchain4jCdiPortableExtFile = findBuildFiles(
                        new File("../../../langchain4j-cdi-portable-ext/target").toPath(),
                        "langchain4j-cdi-portable-ext-")
                .get()
                .toFile();
        File langchain4jCdiCoreFile = findBuildFiles(
                        new File("../../../langchain4j-cdi-core/target").toPath(), "langchain4j-cdi-core-")
                .get()
                .toFile();

        File[] deps = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .importRuntimeDependencies()
                .resolve(
                        "dev.langchain4j.cdi:langchain4j-cdi-portable-ext",
                        "dev.langchain4j:langchain4j-agentic",
                        "org.assertj:assertj-core")
                .withTransitivity()
                .asFile();

        File[] fixedDeps = Stream.concat(
                        Stream.of(langchain4jCdiPortableExtFile, langchain4jCdiCoreFile),
                        Stream.of(deps).filter(f -> !f.getName().startsWith("langchain4j-cdi-")))
                .toArray(File[]::new);

        return ShrinkWrap.create(WebArchive.class, "thinking-test.war")
                .addClasses(
                        ThinkingArquillianTest.class,
                        ThinkingChatAiService.class,
                        ListenerChatAiService.class,
                        ThinkingChatRestService.class,
                        ThinkingChatModelMock.class,
                        ThinkingListener.class,
                        ThinkingTestHelper.class,
                        JaxRsApplication.class,
                        DummyLLConfig.class,
                        ChatModelMock.class,
                        EmbeddingStoreString.class,
                        EmbeddingStoreTextSegment.class)
                .addAsLibraries(fixedDeps)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("llm-config.properties")
                .addAsResource("META-INF/services/jakarta.enterprise.inject.spi.Extension")
                .addAsResource("META-INF/services/dev.langchain4j.cdi.core.config.spi.LLMConfig");
    }

    private static Optional<Path> findBuildFiles(Path folder, String prefix) throws IOException {
        return Files.find(
                        folder,
                        1,
                        (BiPredicate<Path, BasicFileAttributes>) (t, u) -> {
                            String fileName = t.getFileName().toString();
                            return fileName.startsWith(prefix) && fileName.endsWith(".jar");
                        },
                        FileVisitOption.FOLLOW_LINKS)
                .findFirst();
    }

    @SuppressWarnings("unused")
    @ArquillianResource
    private URL baseURL;

    @Test
    public void testOnThinkingHandlerCapturesThinking() {
        try (Client client = ClientBuilder.newClient()) {
            String result = ThinkingTestHelper.postAndGetThinking(client.target(baseURL.toString()), "on-thinking");
            assertThat(result).isEqualTo(ThinkingChatModelMock.THINKING_TEXT);
        }
    }

    @Test
    public void testListenerNameCapturesThinking() {
        try (Client client = ClientBuilder.newClient()) {
            String result = ThinkingTestHelper.postAndGetThinking(client.target(baseURL.toString()), "listener");
            assertThat(result).isEqualTo(ThinkingChatModelMock.THINKING_TEXT);
        }
    }
}
