package dev.langchain4j.cdi.mcp.integrationtests.wildfly;

import dev.langchain4j.cdi.mcp.integrationtests.ArquillianDeploymentHelper;
import dev.langchain4j.cdi.mcp.integrationtests.ConfigResource;
import dev.langchain4j.cdi.mcp.integrationtests.DayPlanPrompt;
import dev.langchain4j.cdi.mcp.integrationtests.ElicitationTool;
import dev.langchain4j.cdi.mcp.integrationtests.GreetingTool;
import dev.langchain4j.cdi.mcp.integrationtests.HeaderParamTool;
import dev.langchain4j.cdi.mcp.integrationtests.IconedTool;
import dev.langchain4j.cdi.mcp.integrationtests.JaxRsApplication;
import dev.langchain4j.cdi.mcp.integrationtests.JdkHttpClientTransport;
import dev.langchain4j.cdi.mcp.integrationtests.JsonRpcAssertions;
import dev.langchain4j.cdi.mcp.integrationtests.McpHttpResponse;
import dev.langchain4j.cdi.mcp.integrationtests.McpHttpTransport;
import dev.langchain4j.cdi.mcp.integrationtests.McpIntegrationTestScenarios;
import dev.langchain4j.cdi.mcp.integrationtests.McpModernTestRequests;
import dev.langchain4j.cdi.mcp.integrationtests.McpTestConstants;
import dev.langchain4j.cdi.mcp.integrationtests.McpTestRequests;
import dev.langchain4j.cdi.mcp.integrationtests.SummarizePrompt;
import dev.langchain4j.cdi.mcp.integrationtests.TaskListTool;
import dev.langchain4j.cdi.mcp.integrationtests.WeatherTool;
import java.io.File;
import java.io.IOException;
import java.net.URL;
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

/**
 * WildFly Arquillian integration tests. Arquillian requires public test methods and does not support test method
 * inheritance, so this class delegates to {@link McpIntegrationTestScenarios}.
 */
@SuppressWarnings({"resource", "java:S5786"})
@ExtendWith(ArquillianExtension.class)
public class McpWildFlyArquillianTest {

    @SuppressWarnings("unused")
    @Deployment
    public static WebArchive createDeployment() throws IOException {
        File mcpServerFile = ArquillianDeploymentHelper.findBuildFile(
                "../../langchain4j-cdi-mcp-server/target", "langchain4j-cdi-mcp-server-");
        File mcpPortableExtFile = ArquillianDeploymentHelper.findBuildFile(
                "../../langchain4j-cdi-mcp-portable-ext/target", "langchain4j-cdi-mcp-portable-ext-");

        File[] deps = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .importRuntimeDependencies()
                .resolve(
                        "dev.langchain4j.cdi:langchain4j-cdi-mcp-portable-ext",
                        "dev.langchain4j:langchain4j-mcp",
                        "org.assertj:assertj-core",
                        "org.mcpjava:mcp-server-api")
                .withTransitivity()
                .asFile();

        File[] fixedDeps = Stream.concat(
                        Stream.of(mcpServerFile, mcpPortableExtFile),
                        Stream.of(deps).filter(f -> !f.getName().startsWith("langchain4j-cdi-mcp-")))
                .toArray(File[]::new);

        return ShrinkWrap.create(WebArchive.class, "mcp-test.war")
                .addClasses(
                        McpWildFlyArquillianTest.class,
                        WeatherTool.class,
                        GreetingTool.class,
                        GreetingTool.Style.class,
                        HeaderParamTool.class,
                        IconedTool.class,
                        IconedTool.CdiIconProvider.class,
                        ElicitationTool.class,
                        ConfigResource.class,
                        SummarizePrompt.class,
                        TaskListTool.class,
                        TaskListTool.Priority.class,
                        DayPlanPrompt.class,
                        JaxRsApplication.class,
                        JdkHttpClientTransport.class,
                        McpHttpTransport.class,
                        McpHttpResponse.class,
                        McpTestRequests.class,
                        McpModernTestRequests.class,
                        McpTestConstants.class,
                        McpIntegrationTestScenarios.class,
                        JsonRpcAssertions.class)
                .addAsLibraries(fixedDeps)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("META-INF/services/jakarta.enterprise.inject.spi.Extension");
    }

    @SuppressWarnings("unused")
    @ArquillianResource
    private URL baseURL;

    private McpIntegrationTestScenarios scenarios() {
        return new McpIntegrationTestScenarios(new JdkHttpClientTransport(baseURL.toString()));
    }

    // --- Tools via MCP Client ---

    @Test
    public void shouldListToolsViaMcpClient() throws Exception {
        scenarios().shouldListToolsViaMcpClient();
    }

    @Test
    public void shouldCallToolViaMcpClient() throws Exception {
        scenarios().shouldCallToolViaMcpClient();
    }

    @Test
    public void shouldCallToolWithOptionalParamOmitted() throws Exception {
        scenarios().shouldCallToolWithOptionalParamOmitted();
    }

    @Test
    public void shouldCallToolWithOptionalParamProvided() throws Exception {
        scenarios().shouldCallToolWithOptionalParamProvided();
    }

    @Test
    public void shouldHaveOptionalParameterInToolSchema() throws Exception {
        scenarios().shouldHaveOptionalParameterInToolSchema();
    }

    @Test
    public void shouldCallToolWithUnknownNameViaMcpClient() throws Exception {
        scenarios().shouldCallToolWithUnknownNameViaMcpClient();
    }

    @Test
    public void shouldListToolsMultipleTimes() throws Exception {
        scenarios().shouldListToolsMultipleTimes();
    }

    @Test
    public void shouldDetectModernProtocolAutomatically() throws Exception {
        scenarios().shouldDetectModernProtocolAutomatically();
    }

    @Test
    public void shouldCallToolViaLegacyClient() throws Exception {
        scenarios().shouldCallToolViaLegacyClient();
    }

    @Test
    public void shouldCallToolViaModernClient() throws Exception {
        scenarios().shouldCallToolViaModernClient();
    }

    @Test
    public void shouldListToolsViaLegacyClient() throws Exception {
        scenarios().shouldListToolsViaLegacyClient();
    }

    // --- Session & HTTP ---

    @Test
    public void shouldInitializeSessionSuccessfully() {
        scenarios().shouldInitializeSessionSuccessfully();
    }

    @Test
    public void shouldSendInitializedNotification() {
        scenarios().shouldSendInitializedNotification();
    }

    @Test
    public void shouldRejectInitializeSentAsNotification() {
        scenarios().shouldRejectInitializeSentAsNotification();
    }

    @Test
    public void shouldHandlePing() {
        scenarios().shouldHandlePing();
    }

    @Test
    public void shouldDeleteSession() {
        scenarios().shouldDeleteSession();
    }

    @Test
    public void shouldReturnCleanJsonForToolCall() {
        scenarios().shouldReturnCleanJsonForToolCall();
    }

    @Test
    public void shouldReturnErrorForUnknownMethod() {
        scenarios().shouldReturnErrorForUnknownMethod();
    }

    @Test
    public void shouldReturnParseErrorForMalformedBody() {
        scenarios().shouldReturnParseErrorForMalformedBody();
    }

    // --- Resources ---

    @Test
    public void shouldListResources() {
        scenarios().shouldListResources();
    }

    @Test
    public void shouldReadResource() {
        scenarios().shouldReadResource();
    }

    @Test
    public void shouldReturnErrorForUnknownResource() {
        scenarios().shouldReturnErrorForUnknownResource();
    }

    // --- Prompts ---

    @Test
    public void shouldListPrompts() {
        scenarios().shouldListPrompts();
    }

    @Test
    public void shouldGetPrompt() {
        scenarios().shouldGetPrompt();
    }

    @Test
    public void shouldReturnErrorForUnknownPrompt() {
        scenarios().shouldReturnErrorForUnknownPrompt();
    }

    // --- Logging ---

    @Test
    public void shouldSetLogLevel() {
        scenarios().shouldSetLogLevel();
    }

    @Test
    public void shouldReturnErrorForInvalidLogLevel() {
        scenarios().shouldReturnErrorForInvalidLogLevel();
    }

    // --- Resource Subscriptions ---

    @Test
    public void shouldSubscribeToResource() {
        scenarios().shouldSubscribeToResource();
    }

    @Test
    public void shouldUnsubscribeFromResource() {
        scenarios().shouldUnsubscribeFromResource();
    }

    // --- Resource Templates ---

    @Test
    public void shouldListResourceTemplates() {
        scenarios().shouldListResourceTemplates();
    }

    // --- Completion ---

    @Test
    public void shouldReturnEmptyCompletionForUnknownRef() {
        scenarios().shouldReturnEmptyCompletionForUnknownRef();
    }

    // --- Notifications ---

    @Test
    public void shouldAcknowledgeCancellation() {
        scenarios().shouldAcknowledgeCancellation();
    }

    @Test
    public void shouldAcceptClientJsonRpcResponse() {
        scenarios().shouldAcceptClientJsonRpcResponse();
    }

    @Test
    public void shouldAcceptAnyNotificationShapedMessage() {
        scenarios().shouldAcceptAnyNotificationShapedMessage();
    }

    // --- Capabilities ---

    @Test
    public void shouldDeclareAllCapabilities() {
        scenarios().shouldDeclareAllCapabilities();
    }

    // --- Negative session tests ---

    @Test
    public void shouldRejectRequestWithInvalidSessionId() {
        scenarios().shouldRejectRequestWithInvalidSessionId();
    }

    @Test
    public void shouldRejectForeignOrigin() {
        scenarios().shouldRejectForeignOrigin();
    }

    @Test
    public void shouldAcceptLocalhostOrigin() {
        scenarios().shouldAcceptLocalhostOrigin();
    }

    // --- Modern protocol (2026-07-28) ---

    @Test
    public void shouldDiscoverSupportedVersions() {
        scenarios().shouldDiscoverSupportedVersions();
    }

    @Test
    public void shouldServeModernRequestsWithoutSession() {
        scenarios().shouldServeModernRequestsWithoutSession();
    }

    @Test
    public void shouldCallToolWithModernRequest() {
        scenarios().shouldCallToolWithModernRequest();
    }

    @Test
    public void shouldRejectMismatchedNameHeader() {
        scenarios().shouldRejectMismatchedNameHeader();
    }

    // --- SEP-2243 ---

    @Test
    public void shouldAcceptMirroredParamHeadersMatchingTheBody() {
        scenarios().shouldAcceptMirroredParamHeadersMatchingTheBody();
    }

    @Test
    public void shouldRejectAParamHeaderDisagreeingWithTheBody() {
        scenarios().shouldRejectAParamHeaderDisagreeingWithTheBody();
    }

    @Test
    public void shouldRejectAnOmittedParamHeaderWhenTheBodyCarriesTheValue() {
        scenarios().shouldRejectAnOmittedParamHeaderWhenTheBodyCarriesTheValue();
    }

    @Test
    public void shouldRejectMalformedBase64ParamHeaderWith400NotWith500() {
        scenarios().shouldRejectMalformedBase64ParamHeaderWith400NotWith500();
    }

    @Test
    public void shouldIgnoreParamHeadersInTheLegacyEra() {
        scenarios().shouldIgnoreParamHeadersInTheLegacyEra();
    }

    // --- Argument binding (langchain4j-cdi#298) ---

    @Test
    public void shouldAdvertiseDefaultedArgumentsAsNotRequired() {
        scenarios().shouldAdvertiseDefaultedArgumentsAsNotRequired();
    }

    @Test
    public void shouldApplyArgumentDefaultsInBothEras() {
        scenarios().shouldApplyArgumentDefaultsInBothEras();
    }

    @Test
    public void shouldBindEnumArgumentInBothEras() {
        scenarios().shouldBindEnumArgumentInBothEras();
    }

    @Test
    public void shouldRejectWronglyTypedToolArgumentAsInvalidParamsInLegacyEra() {
        scenarios().shouldRejectWronglyTypedToolArgumentAsInvalidParamsInLegacyEra();
    }

    @Test
    public void shouldReportWronglyTypedToolArgumentAsToolErrorInModernEra() {
        scenarios().shouldReportWronglyTypedToolArgumentAsToolErrorInModernEra();
    }

    @Test
    public void shouldRejectWronglyTypedPromptArgumentAsInvalidParamsInBothEras() {
        scenarios().shouldRejectWronglyTypedPromptArgumentAsInvalidParamsInBothEras();
    }

    @Test
    public void shouldRejectUnsupportedProtocolVersion() {
        scenarios().shouldRejectUnsupportedProtocolVersion();
    }

    @Test
    public void shouldReturn404ForLegacyOnlyMethodInModernEra() {
        scenarios().shouldReturn404ForLegacyOnlyMethodInModernEra();
    }

    @Test
    public void shouldAcceptModernNotificationWith202() {
        scenarios().shouldAcceptModernNotificationWith202();
    }

    @Test
    public void shouldRoundTripElicitationWithMrtr() {
        scenarios().shouldRoundTripElicitationWithMrtr();
    }

    @Test
    public void shouldRequireDeclaredElicitationCapability() {
        scenarios().shouldRequireDeclaredElicitationCapability();
    }

    @Test
    public void shouldAcknowledgeListenSubscription() throws Exception {
        scenarios().shouldAcknowledgeListenSubscription();
    }

    @Test
    public void shouldRejectListenWithMismatchedProtocolVersionHeader() {
        scenarios().shouldRejectListenWithMismatchedProtocolVersionHeader();
    }
}
