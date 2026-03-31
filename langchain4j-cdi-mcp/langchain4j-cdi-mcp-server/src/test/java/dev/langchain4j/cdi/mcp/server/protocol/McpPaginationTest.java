package dev.langchain4j.cdi.mcp.server.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class McpPaginationTest {

    @Test
    void shouldReturnAllItemsWhenBelowPageSize() {
        List<String> items = List.of("a", "b", "c");
        McpPagination.Page<String> page = McpPagination.paginate(items, null);

        assertThat(page.items()).containsExactly("a", "b", "c");
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void shouldPaginateWithCursor() {
        List<String> items = List.of("a", "b", "c", "d", "e");
        McpPagination.Page<String> page = McpPagination.paginate(items, null, 2);

        assertThat(page.items()).containsExactly("a", "b");
        assertThat(page.nextCursor()).isNotNull();

        McpPagination.Page<String> page2 = McpPagination.paginate(items, page.nextCursor(), 2);
        assertThat(page2.items()).containsExactly("c", "d");
        assertThat(page2.nextCursor()).isNotNull();

        McpPagination.Page<String> page3 = McpPagination.paginate(items, page2.nextCursor(), 2);
        assertThat(page3.items()).containsExactly("e");
        assertThat(page3.nextCursor()).isNull();
    }

    @Test
    void shouldHandleInvalidCursorGracefully() {
        List<String> items = List.of("a", "b");
        McpPagination.Page<String> page = McpPagination.paginate(items, "invalid-cursor");

        assertThat(page.items()).containsExactly("a", "b");
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void shouldHandleEmptyList() {
        McpPagination.Page<String> page = McpPagination.paginate(List.of(), null);

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void shouldEncodeThenDecodeCursor() {
        String cursor = McpPagination.encodeCursor(42);
        int offset = McpPagination.decodeOffset(cursor);
        assertThat(offset).isEqualTo(42);
    }
}
