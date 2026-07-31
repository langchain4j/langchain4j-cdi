package dev.langchain4j.cdi.core.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.cdi.integrationtests.AgentChatRestService;
import dev.langchain4j.cdi.integrationtests.AgentChatService;
import dev.langchain4j.cdi.integrationtests.ChatAiService;
import dev.langchain4j.cdi.integrationtests.ChatRestService;
import dev.langchain4j.cdi.integrationtests.GuardrailChatAiService;
import dev.langchain4j.cdi.integrationtests.GuardrailChatRestService;
import dev.langchain4j.cdi.integrationtests.NoEmptyMessageInputGuardrail;
import dev.langchain4j.cdi.integrationtests.PassThroughOutputGuardrail;
import dev.langchain4j.cdi.integrationtests.ThinkingChatModelMock;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
public class ChatRestServiceArquillianTest {

    @SuppressWarnings("unused")
    @Deployment
    public static WebArchive createDeployment() throws IOException {
        // Include the application classes and the portable extension as a library
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

        return ShrinkWrap.create(WebArchive.class, "chat-test.war")
                .addClasses(
                        ChatRestServiceArquillianTest.class,
                        ChatAiService.class,
                        ChatRestService.class,
                        GuardrailChatAiService.class,
                        GuardrailChatRestService.class,
                        NoEmptyMessageInputGuardrail.class,
                        PassThroughOutputGuardrail.class,
                        AgentChatService.class,
                        AgentChatRestService.class,
                        JaxRsApplication.class,
                        DummyLLConfig.class,
                        ChatModelMock.class,
                        EmbeddingStoreString.class,
                        EmbeddingStoreTextSegment.class,
                        ThinkingChatModelMock.class)
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
    public void testChatRestService() {
        String chatEndpoint = baseURL + "chat";
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(chatEndpoint);

            String question = "What is the meaning of life?";
            Response response = target.request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(question, MediaType.APPLICATION_JSON));

            assertThat(response.getStatus()).isEqualTo(200);
            String result = response.readEntity(String.class);
            assertThat(result).isNotNull().isEqualTo("ok EmbeddingStoreTextSegment{}");
        }
    }

    @Test
    public void testAgentChatRestService() {
        String chatEndpoint = baseURL + "agent-chat";
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(chatEndpoint);

            String question = "What is the meaning of life?";
            Response response = target.request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(question, MediaType.APPLICATION_JSON));

            assertThat(response.getStatus()).isEqualTo(200);
            String result = response.readEntity(String.class);
            assertThat(result).isNotNull().isEqualTo("ok EmbeddingStoreTextSegment{}");
        }
    }

    @Test
    public void testGuardrailChatWithValidMessage() {
        String chatEndpoint = baseURL + "guarded-chat";
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(chatEndpoint);

            String question = "What is the meaning of life?";
            Response response = target.request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(question, MediaType.APPLICATION_JSON));

            assertThat(response.getStatus()).isEqualTo(200);
            String result = response.readEntity(String.class);
            // Jakarta EE ChatModelMock includes embeddingStore in response (configured via llm-config.properties)
            assertThat(result).isNotNull().isEqualTo("ok EmbeddingStoreTextSegment{}");
        }
    }

    @Test
    public void testGuardrailChatWithEmptyMessage() {
        String chatEndpoint = baseURL + "guarded-chat";
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(chatEndpoint);

            Response response =
                    target.request(MediaType.APPLICATION_JSON).post(Entity.entity("", MediaType.APPLICATION_JSON));

            assertThat(response.getStatus()).isEqualTo(400);
        }
    }
}
