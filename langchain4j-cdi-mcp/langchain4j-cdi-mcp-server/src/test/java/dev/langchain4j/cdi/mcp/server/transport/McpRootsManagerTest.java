package dev.langchain4j.cdi.mcp.server.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpRootsManagerTest {

    McpRootsManager rootsManager;
    McpServerRequestManager requestManager;

    @BeforeEach
    void setup() throws Exception {
        rootsManager = new McpRootsManager();
        requestManager = mock(McpServerRequestManager.class);
        Field field = McpRootsManager.class.getDeclaredField("requestManager");
        field.setAccessible(true);
        field.set(rootsManager, requestManager);
    }

    @Test
    void shouldRequestAndCacheRoots() {
        JsonObject result = Json.createObjectBuilder()
                .add(
                        "roots",
                        Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("uri", "file:///home/user/project")
                                        .add("name", "My Project")))
                .build();
        when(requestManager.sendRequest(eq("session-1"), eq("roots/list"), any()))
                .thenReturn(result);

        List<org.mcp_java.model.roots.Root> roots = rootsManager.requestRoots("session-1");

        assertThat(roots).hasSize(1);
        assertThat(roots.get(0).uri()).isEqualTo("file:///home/user/project");
        assertThat(roots.get(0).name()).isEqualTo("My Project");

        // Should be cached
        assertThat(rootsManager.getRoots("session-1")).hasSize(1);
    }

    @Test
    void shouldReturnEmptyListOnTimeout() {
        when(requestManager.sendRequest(any(), any(), any())).thenReturn(null);

        List<org.mcp_java.model.roots.Root> roots = rootsManager.requestRoots("session-1");

        assertThat(roots).isEmpty();
    }

    @Test
    void shouldRemoveSessionRoots() {
        JsonObject result = Json.createObjectBuilder()
                .add(
                        "roots",
                        Json.createArrayBuilder().add(Json.createObjectBuilder().add("uri", "file:///tmp")))
                .build();
        when(requestManager.sendRequest(any(), any(), any())).thenReturn(result);

        rootsManager.requestRoots("session-1");
        assertThat(rootsManager.getRoots("session-1")).hasSize(1);

        rootsManager.removeSession("session-1");
        assertThat(rootsManager.getRoots("session-1")).isEmpty();
    }
}
